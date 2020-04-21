/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.refactoring.rename

import com.intellij.lang.Language
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.usageView.UsageInfo
import com.jetbrains.cidr.CidrLog
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.psi.OCQualifiedExpression
import com.jetbrains.swift.SwiftLanguage
import org.jetbrains.konan.resolve.konan.KonanConsumer
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer
import org.jetbrains.kotlin.backend.konan.objcexport.abbreviate
import org.jetbrains.kotlin.backend.konan.objcexport.createNamer
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.project.toDescriptor
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.refactoring.rename.ForeignUsagesRenameProcessor
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.*
import org.jetbrains.plugins.gradle.execution.build.CachedModuleDataFinder

class KotlinUsagesInSwiftAndObjCRenameProcessor : ForeignUsagesRenameProcessor() {
    override fun process(element: PsiElement, newName: String, language: Language, allUsages: Collection<UsageInfo>): Boolean {
        if (allUsages.isEmpty() || element !is KtElement || language != OCLanguage.getInstance() && language != SwiftLanguage.INSTANCE) {
            return false
        }

        val moduleGraph = ModuleManager.getInstance(element.project).moduleGraph()
        val allModules = element.module?.let { ModuleUtil.getAllDependentModules(it).toHashSet() } ?: return false
        for ((module, usages) in allUsages.groupBy { it.element?.module }) {
            if (module == null) continue

            val nativeModules = moduleGraph.getIn(module).asSequence()
                .filter { it in allModules }
                .filter { KotlinFacet.get(it)?.configuration?.settings?.targetPlatform?.isNative() ?: false }
                .toList()
            CidrLog.LOG.assertTrue(nativeModules.size == 1)

            val namer = createNamer(nativeModules.first(), element) ?: continue

            val newNameIdentifier = Name.identifier(newName)
            namer.getNewNameForUsage(element, newNameIdentifier, language)?.let { name ->
                usages.forEach { RenameUtil.rename(it, name) }
            }

            if (language == OCLanguage.getInstance() && element is KtObjectDeclaration && element.isCompanion()) {
                val renamedDescriptor = element.renamedDescriptor(newNameIdentifier)
                for (usage in usages) {
                    val expression = PsiTreeUtil.getParentOfType(usage.reference?.element, OCQualifiedExpression::class.java)
                    expression?.reference?.handleElementRename(namer.getObjectInstanceSelector(renamedDescriptor))
                }
            }
        }
        return true
    }

    private fun ObjCExportNamer.getNewNameForUsage(element: PsiElement, newName: Name, language: Language) = when (element) {
        is KtEnumEntry -> getEnumEntrySelector(element.renamedDescriptor(newName))
        is KtProperty -> getPropertyName(element.renamedDescriptor(newName))
        is KtClassOrObject -> {
            val objcAndSwiftName = getClassOrProtocolName(element.renamedDescriptor(newName))
            if (language == SwiftLanguage.INSTANCE) objcAndSwiftName.swiftName.split('.').last() else objcAndSwiftName.objCName
        }
        is KtNamedFunction -> {
            val descriptor = element.renamedDescriptor(newName)
            if (language == SwiftLanguage.INSTANCE) getSwiftName(descriptor).split(':').first() else getSelector(descriptor)
        }
        else -> null
    }

    private fun createNamer(nativeModule: Module, element: KtElement): ObjCExportNamer? {
        val moduleNode = CachedModuleDataFinder().findMainModuleData(nativeModule) ?: return null
        val framework =
            KonanConsumer.getAllReferencedKonanTargets(element.project).firstOrNull { it.moduleId == moduleNode.data.id } ?: return null
        val nativeModuleDescriptor = nativeModule.toDescriptor() ?: return null
        val exportedDependencies = element.module?.toDescriptor()?.let { listOf(it) } ?: return null
        return createNamer(nativeModuleDescriptor, exportedDependencies, abbreviate(framework.productModuleName))
    }

    private fun KtClassOrObject.renamedDescriptor(name: Name): ClassDescriptor {
        return object : ClassDescriptor by descriptor as ClassDescriptor {
            override fun getName(): Name = name
        }
    }

    private fun KtNamedFunction.renamedDescriptor(name: Name): FunctionDescriptor {
        return object : FunctionDescriptor by descriptor as FunctionDescriptor {
            override fun getName(): Name = name
        }
    }

    private fun KtProperty.renamedDescriptor(name: Name): PropertyDescriptor {
        return object : PropertyDescriptor by descriptor as PropertyDescriptor {
            override fun getName(): Name = name
        }
    }
}