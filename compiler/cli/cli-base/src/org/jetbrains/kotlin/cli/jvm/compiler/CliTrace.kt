/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.jvm.JvmCodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

class CliTraceHolder : JvmCodeAnalyzerInitializer() {
    lateinit var bindingContext: BindingContext
        private set
    lateinit var module: ModuleDescriptor
        private set
    lateinit var languageVersionSettings: LanguageVersionSettings
        private set
    lateinit var jvmTarget: JvmTarget
        private set

    override fun initialize(
        trace: BindingTrace,
        module: ModuleDescriptor,
        codeAnalyzer: KotlinCodeAnalyzer,
        languageVersionSettings: LanguageVersionSettings,
        jvmTarget: JvmTarget
    ) {
        this.bindingContext = trace.bindingContext
        this.module = module
        this.languageVersionSettings = languageVersionSettings
        this.jvmTarget = jvmTarget

        if (trace !is CliBindingTrace) {
            throw IllegalArgumentException("Shared trace is expected to be subclass of ${CliBindingTrace::class.java.simpleName} class")
        }

        trace.setKotlinCodeAnalyzer(codeAnalyzer)
    }

    override fun createTrace(): BindingTraceContext {
        return NoScopeRecordCliBindingTrace()
    }
}


// TODO: needs better name + list of keys to skip somewhere
class NoScopeRecordCliBindingTrace : CliBindingTrace() {
    override fun <K, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
        if (slice == BindingContext.LEXICAL_SCOPE || slice == BindingContext.DATA_FLOW_INFO_BEFORE) {
            // In the compiler there's no need to keep scopes
            return
        }
        super.record(slice, key, value)
    }

    override fun toString(): String {
        return NoScopeRecordCliBindingTrace::class.java.name
    }
}

open class CliBindingTrace @TestOnly constructor() : BindingTraceContext() {
    private var kotlinCodeAnalyzer: KotlinCodeAnalyzer? = null

    override fun toString(): String {
        return CliBindingTrace::class.java.name
    }

    fun setKotlinCodeAnalyzer(kotlinCodeAnalyzer: KotlinCodeAnalyzer) {
        this.kotlinCodeAnalyzer = kotlinCodeAnalyzer
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        val value = super.get(slice, key)

        if (value == null) {
            if (key is KtDeclaration) {
                // NB: intentional code duplication, see https://youtrack.jetbrains.com/issue/KT-43296
                if (BindingContext.FUNCTION === slice) {
                    if (!KtPsiUtil.isLocal(key)) {
                        kotlinCodeAnalyzer!!.resolveToDescriptor(key)
                        return super.get(slice, key) as V?
                    }
                }
                if (BindingContext.VARIABLE === slice) {
                    if (!KtPsiUtil.isLocal(key)) {
                        kotlinCodeAnalyzer!!.resolveToDescriptor(key)
                        return super.get(slice, key) as V?
                    }
                }
            }
        }

        return value
    }
}
