/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.util.Key
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.KotlinExtraDiagnosticsProvider
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.buildLightClass
import org.jetbrains.kotlin.asJava.classes.getOutermostClassOrObject
import org.jetbrains.kotlin.asJava.classes.safeIsLocal
import org.jetbrains.kotlin.asJava.classes.safeScript
import org.jetbrains.kotlin.asJava.classes.shouldNotBeVisibleAsLightClass
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.utils.addToStdlib.cast

private val JAVA_API_STUB_FOR_SCRIPT = Key.create<CachedValue<LightClassDataHolder.ForScript>>("JAVA_API_STUB_FOR_SCRIPT")
private val JAVA_API_STUB = Key.create<CachedValue<LightClassDataHolder.ForClass>>("JAVA_API_STUB")
private val JAVA_API_STUB_LOCK = Key.create<Any>("JAVA_API_STUB_LOCK")

private val javaApiStubInitIsRunning: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

class CliExtraDiagnosticsProvider : KotlinExtraDiagnosticsProvider {
    override fun forClassOrObject(kclass: KtClassOrObject): Diagnostics {
        if (kclass.shouldNotBeVisibleAsLightClass()) {
            return Diagnostics.EMPTY
        }

        val containingScript = kclass.containingKtFile.safeScript()
        return when {
            !kclass.safeIsLocal() && containingScript != null -> getLightClassCachedValue(containingScript).value
            else -> getLightClassCachedValue(kclass).value
        }.extraDiagnostics
    }

    override fun forFacade(file: KtFile, moduleScope: GlobalSearchScope): Diagnostics {
        val project = file.project
        val facadeFqName = JvmFileClassUtil.getFileClassInfoNoResolve(file).facadeClassFqName
        val files = KotlinAsJavaSupport.getInstance(project)
            .findFilesForFacade(facadeFqName, moduleScope)
            .ifEmpty { return Diagnostics.EMPTY }

        return LightClassGenerationSupport.getInstance(project).cast<CliLightClassGenerationSupport>()
            .createDataHolderForFacade { constructionContext ->
                buildLightClass(facadeFqName.parent(), files, ClassFilterForFacade, constructionContext) generate@{ state, files ->
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
            }.extraDiagnostics
    }
}

private fun getLightClassCachedValue(script: KtScript): CachedValue<LightClassDataHolder.ForScript> {
    return script.getUserData(JAVA_API_STUB_FOR_SCRIPT) ?: createCachedValueForScript(script).also {
        script.putUserData(JAVA_API_STUB_FOR_SCRIPT, it)
    }
}

private fun createCachedValueForScript(script: KtScript): CachedValue<LightClassDataHolder.ForScript> =
    CachedValuesManager.getManager(script.project).createCachedValue(LightClassDataProviderForScript(script), false)

private fun getLightClassCachedValue(classOrObject: KtClassOrObject): CachedValue<LightClassDataHolder.ForClass> {
    val outerClassValue = getOutermostClassOrObject(classOrObject).getUserData(JAVA_API_STUB)
    outerClassValue?.let {
        // stub computed for outer class can be used for inner/nested
        return it
    }
    // the idea behind this locking approach:
    // Thread T1 starts to calculate value for A it acquires lock for A
    //
    // Assumption 1: Lets say A calculation requires another value e.g. B to be calculated
    // Assumption 2: Thread T2 wants to calculate value for B

    // to avoid dead-lock case we mark thread as doing calculation and acquire lock only once per thread
    // as a trade-off to prevent dependent value could be calculated several time
    // due to CAS (within putUserDataIfAbsent etc) the same instance of calculated value will be used
    val value: CachedValue<LightClassDataHolder.ForClass> = if (!javaApiStubInitIsRunning.get()) {
        classOrObject.getUserData(JAVA_API_STUB) ?: run {
            val lock = classOrObject.putUserDataIfAbsent(JAVA_API_STUB_LOCK, Object())
            synchronized(lock) {
                try {
                    javaApiStubInitIsRunning.set(true)
                    computeLightClassCachedValue(classOrObject)
                } finally {
                    javaApiStubInitIsRunning.set(false)
                }
            }
        }
    } else {
        computeLightClassCachedValue(classOrObject)
    }

    return value
}

private fun computeLightClassCachedValue(classOrObject: KtClassOrObject): CachedValue<LightClassDataHolder.ForClass> {
    val value = classOrObject.getUserData(JAVA_API_STUB) ?: run {
        val manager = CachedValuesManager.getManager(classOrObject.project)
        val cachedValue = manager.createCachedValue(
            LightClassDataProviderForClassOrObject(classOrObject), false
        )
        classOrObject.putUserDataIfAbsent(JAVA_API_STUB, cachedValue)
    }
    return value
}