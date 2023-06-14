/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.differences

import org.jetbrains.kotlin.android.synthetic.diagnostic.ErrorsAndroid
import org.jetbrains.kotlin.assignment.plugin.diagnostics.ErrorsAssignmentPlugin
import org.jetbrains.kotlin.assignment.plugin.k2.diagnostics.FirErrorsAssignmentPlugin
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.fir.plugin.checkers.PluginErrors
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs
import org.jetbrains.kotlin.noarg.diagnostic.ErrorsNoArg
import org.jetbrains.kotlin.noarg.fir.KtErrorsNoArg
import org.jetbrains.kotlin.parcelize.diagnostic.ErrorsParcelize
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.konan.diagnostics.ErrorsNative
import org.jetbrains.kotlin.wasm.resolve.diagnostics.ErrorsWasm
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.SerializationErrors
import org.jetbrains.kotlinx.serialization.compiler.fir.checkers.FirSerializationErrors
import kotlin.reflect.KClass

fun getNonErrorFromFieldValue(value: Any?): String? {
    return when (value) {
        is AbstractKtDiagnosticFactory -> when (value.severity) {
            Severity.ERROR -> null
            else -> value.name
        }
        is KtDiagnosticFactoryForDeprecation<*> -> value.warningFactory.name
        is DiagnosticFactoryWithPsiElement<*, *> -> when (value.severity) {
            Severity.ERROR -> null
            else -> value.name
        }
        is DummyDelegate<*> -> getNonErrorFromFieldValue(value.value)
        else -> null
    }
}

fun getErrorFromFieldValue(value: Any?): String? {
    return when (value) {
        is AbstractKtDiagnosticFactory -> when (value.severity) {
            Severity.ERROR -> value.name
            else -> null
        }
        is KtDiagnosticFactoryForDeprecation<*> -> value.errorFactory.name
        is DiagnosticFactoryWithPsiElement<*, *> -> when (value.severity) {
            Severity.ERROR -> value.name
            else -> null
        }
        is DummyDelegate<*> -> getErrorFromFieldValue(value.value)
        else -> null
    }
}

inline fun collectFromFieldsOf(klassWithErrors: KClass<*>, instance: Any?, collector: (Any?) -> String?): List<String> {
    // NB: trying to use `kotlin.reflect.full.memberProperties`
    // fails with `Built-in class kotlin.Any is not found`,
    // and I'm not sure why
    return klassWithErrors.java.declaredFields.mapNotNull { field ->
        // Otherwise can't access kotlin objects
        field.isAccessible = true
        collector(field.get(instance))
    }
}

fun collectNonErrorsFromFieldsOf(klassWithErrors: KClass<*>, instance: Any?) =
    collectFromFieldsOf(klassWithErrors, instance, ::getNonErrorFromFieldValue)

fun collectAllK1NonErrors(): Set<String> {
    val diagnosticsFromInterfaces = listOf(
        Errors::class, ErrorsJvm::class, ErrorsJs::class, ErrorsWasm::class,
        ErrorsParcelize::class, SerializationErrors::class, ErrorsNoArg::class, ErrorsAssignmentPlugin::class,
        ErrorsAndroid::class,
    ).flatMap {
        collectNonErrorsFromFieldsOf(it, instance = null)
    }.toSet()
    val diagnosticsFromObjects = listOf(ErrorsNative)
        .flatMap { collectNonErrorsFromFieldsOf(it::class, instance = it) }
        .toSet()
    return diagnosticsFromInterfaces + diagnosticsFromObjects
}

fun collectAllK2NonErrors(): Set<String> {
    return listOf(
        FirErrors, FirJvmErrors, FirJsErrors, FirNativeErrors, FirSyntaxErrors,
        KtErrorsParcelize, FirSerializationErrors, KtErrorsNoArg, FirErrorsAssignmentPlugin,
        PluginErrors,
    ).flatMap {
        collectNonErrorsFromFieldsOf(it::class, instance = it)
    }.toSet()
}
