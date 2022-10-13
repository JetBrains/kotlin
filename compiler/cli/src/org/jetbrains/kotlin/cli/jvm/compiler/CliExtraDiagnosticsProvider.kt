/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.util.Key
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.classes.getOutermostClassOrObject
import org.jetbrains.kotlin.asJava.classes.shouldNotBeVisibleAsLightClass
import org.jetbrains.kotlin.cli.jvm.compiler.builder.extraJvmDiagnosticsFromBackend
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

private val JAVA_API_STUB = Key.create<CachedValue<Diagnostics>>("JAVA_API_STUB")

object CliExtraDiagnosticsProvider {
    fun forClassOrObject(kclass: KtClassOrObject): Diagnostics {
        if (kclass.shouldNotBeVisibleAsLightClass()) {
            return Diagnostics.EMPTY
        }

        return getLightClassCachedValue(kclass).value
    }

    fun forFacade(file: KtFile): Diagnostics = CachedValuesManager.getCachedValue(file) {
        CachedValueProvider.Result.create(
            calculateForFacade(file),
            KotlinModificationTrackerService.getInstance(file.project).outOfBlockModificationTracker,
        )
    }

    private fun calculateForFacade(file: KtFile): Diagnostics {
        val project = file.project
        val facadeFqName = file.javaFileFacadeFqName
        val facadeCollection = KotlinAsJavaSupport.getInstance(project)
            .findFilesForFacade(facadeFqName, GlobalSearchScope.allScope(project))
            .ifEmpty { return Diagnostics.EMPTY }

        val context = (LightClassGenerationSupport.getInstance(project) as CliLightClassGenerationSupport).context
        val (_, _, diagnostics) = extraJvmDiagnosticsFromBackend(
            facadeFqName.parent(),
            facadeCollection,
            ClassFilterForFacade,
            context,
        ) generate@{ state, files ->
            val representativeFile = files.first()
            val fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(representativeFile)
            if (!fileClassInfo.withJvmMultifileClass) {
                val codegen = state.factory.forPackage(representativeFile.packageFqName, files)
                codegen.generate()
                state.factory.done()
                return@generate
            }

            val codegen = state.factory.forMultifileClass(facadeFqName, files)
            codegen.generate()
            state.factory.done()
        }

        return diagnostics
    }
}

private fun getLightClassCachedValue(classOrObject: KtClassOrObject): CachedValue<Diagnostics> {
    val outerClassValue = getOutermostClassOrObject(classOrObject).getUserData(JAVA_API_STUB)
    outerClassValue?.let {
        // stub computed for outer class can be used for inner/nested
        return it
    }

    return computeLightClassCachedValue(classOrObject)
}

private fun computeLightClassCachedValue(classOrObject: KtClassOrObject): CachedValue<Diagnostics> {
    val value = classOrObject.getUserData(JAVA_API_STUB) ?: run {
        val manager = CachedValuesManager.getManager(classOrObject.project)
        val cachedValue = manager.createCachedValue(LightClassDataProviderForClassOrObject(classOrObject))

        classOrObject.putUserDataIfAbsent(JAVA_API_STUB, cachedValue)
    }

    return value
}
