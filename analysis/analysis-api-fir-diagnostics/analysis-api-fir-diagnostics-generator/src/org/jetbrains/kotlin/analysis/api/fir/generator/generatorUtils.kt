/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator

import org.jetbrains.kotlin.utils.SmartPrinter
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

internal fun SmartPrinter.printTypeWithShortNames(type: KType, shouldRenderFqName: (KType) -> Boolean = { false }) {
    fun typeConversion(type: KType): String {
        val nullableSuffix = if (type.isMarkedNullable) "?" else ""
        val simpleName = if (shouldRenderFqName(type)) {
            type.qualifiedName
        } else {
            type.simpleName
        }
        return if (type.arguments.isEmpty()) simpleName + nullableSuffix
        else simpleName + type.arguments.joinToString(separator = ", ", prefix = "<", postfix = ">") {
            when (val typeArgument = it.type) {
                null -> "*"
                else -> typeConversion(typeArgument)
            } + nullableSuffix
        }
    }
    print(typeConversion(type))
}

val KType.simpleName: String
    get() = when (val classifier = classifier) {
        is DiagnosticApiClass -> classifier.name
        else -> (classifier as KClass<*>).simpleName!!
    }

val KType.qualifiedName: String
    get() = when (val classifier = classifier) {
        // The class is in the package of the generated diagnostics, so its simple name is always sufficient
        is DiagnosticApiClass -> classifier.name
        else -> (classifier as KClass<*>).qualifiedName!!
    }

/**
 * A class declared in the module the diagnostics are generated into, in the package of the diagnostics.
 *
 * The generator cannot depend on the module it generates sources for, so such a class is known only by its [name].
 */
internal class DiagnosticApiClass(val name: String) : KClassifier

/**
 * A type built by the generator. Unlike the types created by `kotlin-reflect`, it can refer to a [DiagnosticApiClass].
 */
internal class HLType(
    override val classifier: KClassifier,
    override val arguments: List<KTypeProjection> = emptyList(),
    override val isMarkedNullable: Boolean = false,
) : KType {
    override val annotations: List<Annotation> get() = emptyList()
}

internal fun diagnosticApiType(name: String): KType = HLType(DiagnosticApiClass(name))
