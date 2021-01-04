/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve.wrappers

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.types.KotlinType

abstract class KtSymbolBasedDeclarationDescriptor<T : KtSymbol>(val ktSymbol: T) : DeclarationDescriptor {
    override val annotations: Annotations
        get() {
            val ktAnnotations = (ktSymbol as? KtAnnotatedSymbol)?.annotations ?: return Annotations.EMPTY
            return Annotations.create(ktAnnotations.map(::KtSymbolBasedAnnotationDescriptor))
        }

    override fun getContainingDeclaration(): DeclarationDescriptor? {
        TODO("SEImplement")
    }

    override fun getOriginal(): DeclarationDescriptor {
        TODO("SEImplement")
    }

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R =
        error("Visiting is not allowed for KtSymbol wrappers")

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?): Unit =
        error("Visiting is not allowed for KtSymbol wrappers")
}

class KtSymbolBasedAnnotationDescriptor(
    private val ktAnnotationCall: KtAnnotationCall
) : AnnotationDescriptor {
    override val type: KotlinType
        get() = TODO("SEImplement")

    override val fqName: FqName?
        get() = ktAnnotationCall.classId?.asSingleFqName()

    override val allValueArguments: Map<Name, ConstantValue<*>> =
        ktAnnotationCall.arguments.associate { Name.identifier(it.name) to it.expression.toConstantValue() }

    override val source: SourceElement
        get() = ktAnnotationCall.psi?.toSourceElement() ?: SourceElement.NO_SOURCE
}

private fun KtConstantValue.toConstantValue(): ConstantValue<*> =
    when (this) {
        KtUnsupportedConstantValue -> ErrorValue.create("Error value for KtUnsupportedConstantValue")
        is KtSimpleConstantValue<*> -> when (val value = constant) {
            null -> NullValue()
            is Boolean -> BooleanValue(value)
            is Char -> CharValue(value)
            is Byte -> ByteValue(value)
            is Short -> ShortValue(value)
            is Int -> IntValue(value)
            is Long -> LongValue(value)
            is String -> StringValue(value)
            is Float -> FloatValue(value)
            is Double -> DoubleValue(value)
            else -> error("Unexpected constant KtSimpleConstantValue: $value (class: ${value.javaClass}")
        }
        is KtUnsignedConstantValue<*> -> when (val value = runtimeConstant) {
            is Byte -> UByteValue(value)
            is Short -> UShortValue(value)
            is Int -> UIntValue(value)
            is Long -> ULongValue(value)
            else -> error("Unexpected constant KtSimpleConstantValue: $value (class: ${value?.javaClass}")
        }
    }



