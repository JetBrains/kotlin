/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.buildLightClass
import org.jetbrains.kotlin.asJava.classes.getOutermostClassOrObject
import org.jetbrains.kotlin.asJava.classes.safeIsLocal
import org.jetbrains.kotlin.codegen.MemberCodegen
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.org.objectweb.asm.Type

internal class LightClassDataProviderForClassOrObject(
    private val classOrObject: KtClassOrObject
) : CachedValueProvider<LightClassDataHolder.ForClass> {
    private fun computeLightClassData(): LightClassDataHolder.ForClass {
        val file = classOrObject.containingKtFile
        val packageFqName = file.packageFqName
        return LightClassGenerationSupport.getInstance(classOrObject.project)
            .createDataHolderForClass(classOrObject) { constructionContext ->
                buildLightClass(
                    packageFqName,
                    listOf(file),
                    ClassFilterForClassOrObject(classOrObject),
                    constructionContext
                ) { state, files ->
                    val packageCodegen = state.factory.forPackage(packageFqName, files)
                    val packagePartType = Type.getObjectType(JvmFileClassUtil.getFileClassInternalName(file))
                    val context = state.rootContext.intoPackagePart(packageCodegen.packageFragment, packagePartType, file)
                    MemberCodegen.genClassOrObject(context, getOutermostClassOrObject(classOrObject), state, null)
                    state.factory.done()
                }
            }
    }

    override fun compute(): CachedValueProvider.Result<LightClassDataHolder.ForClass>? {
        val trackerService = KotlinModificationTrackerService.getInstance(classOrObject.project)
        return CachedValueProvider.Result.create(
            computeLightClassData(),
            if (classOrObject.safeIsLocal()) trackerService.modificationTracker else trackerService.outOfBlockModificationTracker
        )
    }

    override fun toString(): String {
        return this::class.java.name + " for " + classOrObject.name
    }
}

private class ClassFilterForClassOrObject(private val classOrObject: KtClassOrObject) : GenerationState.GenerateClassFilter() {

    override fun shouldGeneratePackagePart(ktFile: KtFile) = true
    override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject) = shouldGenerateClass(processingClassOrObject)

    override fun shouldGenerateClassMembers(processingClassOrObject: KtClassOrObject): Boolean {
        if (classOrObject === processingClassOrObject) return true

        // process all children
        if (classOrObject.isAncestor(processingClassOrObject, true)) {
            return true
        }

        // Local classes should be process by CodegenAnnotatingVisitor to
        // decide what class they should be placed in.
        //
        // Example:
        // class A
        // fun foo() {
        //     trait Z: A {}
        //     fun bar() {
        //         class <caret>O2: Z {}
        //     }
        // }
        // TODO: current method will process local classes in irrelevant declarations, it should be fixed.
        // We generate all enclosing classes

        if (classOrObject.safeIsLocal() && processingClassOrObject.safeIsLocal()) {
            val commonParent = PsiTreeUtil.findCommonParent(classOrObject, processingClassOrObject)
            return commonParent != null && commonParent !is PsiFile
        }

        return false
    }

    override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject)
    // generate outer classes but not their members
            = shouldGenerateClassMembers(processingClassOrObject) || processingClassOrObject.isAncestor(classOrObject, true)

    override fun shouldGenerateScript(script: KtScript) = PsiTreeUtil.isAncestor(script, classOrObject, false)
    override fun shouldGenerateCodeFragment(script: KtCodeFragment) = false
}

internal class LightClassDataProviderForScript(private val script: KtScript) : CachedValueProvider<LightClassDataHolder.ForScript> {
    private fun computeLightClassData(): LightClassDataHolder.ForScript {
        return LightClassGenerationSupport.getInstance(script.project).createDataHolderForScript(script) { constructionContext ->
            buildLightClass(
                script.fqName.parent(),
                listOf(script.containingKtFile),
                ClassFilterForScript(script),
                constructionContext
            ) generate@{ state, files ->
                val scriptFile = files.first()
                val codegen = state.factory.forPackage(scriptFile.packageFqName, files)
                codegen.generate()
                state.factory.done()
            }
        }
    }

    override fun compute(): CachedValueProvider.Result<LightClassDataHolder.ForScript> =
        CachedValueProvider.Result.create(
            computeLightClassData(),
            KotlinModificationTrackerService.getInstance(script.project).outOfBlockModificationTracker
        )

    override fun toString(): String = this::class.java.name + " for ${script.fqName}"
}

private class ClassFilterForScript(val script: KtScript) : GenerationState.GenerateClassFilter() {
    override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean =
        shouldGenerateClass(processingClassOrObject)

    override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean =
        processingClassOrObject.isAncestor(script, true)

    override fun shouldGenerateClassMembers(processingClassOrObject: KtClassOrObject): Boolean = true

    override fun shouldGeneratePackagePart(ktFile: KtFile): Boolean = script.containingKtFile === ktFile

    override fun shouldGenerateScript(script: KtScript): Boolean = this.script === script
    override fun shouldGenerateCodeFragment(script: KtCodeFragment) = false
}

internal object ClassFilterForFacade : GenerationState.GenerateClassFilter() {
    override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject) = shouldGenerateClass(processingClassOrObject)
    override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject) = KtPsiUtil.isLocal(processingClassOrObject)
    override fun shouldGeneratePackagePart(ktFile: KtFile) = true
    override fun shouldGenerateScript(script: KtScript) = false
    override fun shouldGenerateCodeFragment(script: KtCodeFragment) = false
}