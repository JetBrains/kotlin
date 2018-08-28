/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object DslMarkerUtils {

    object FunctionTypeAnnotationsKey : FunctionDescriptor.UserDataKey<Annotations>

    data class DslMarkersFromReceiver(
        val common: Set<FqName>,
        val fromContainingFunctionType: Set<FqName>
    ) {
        fun all() = common + fromContainingFunctionType
    }

    fun extractDslMarkerFqNames(receiver: ReceiverValue): DslMarkersFromReceiver {
        val errorLevel = extractDslMarkerFqNames(receiver.type)

        val deprecationLevel =
            receiver.safeAs<ExtensionReceiver>()
                ?.declarationDescriptor
                ?.safeAs<FunctionDescriptor>()
                ?.getUserData(FunctionTypeAnnotationsKey)
                ?.let(Annotations::extractDslMarkerFqNames)
                ?.toSet() ?: emptySet()

        return DslMarkersFromReceiver(errorLevel, deprecationLevel)
    }

    fun extractDslMarkerFqNames(kotlinType: KotlinType): Set<FqName> {
        val result = mutableSetOf<FqName>()

        result.addAll(kotlinType.annotations.extractDslMarkerFqNames())

        kotlinType.getAbbreviation()?.constructor?.declarationDescriptor?.run {
            result.addAll(annotations.extractDslMarkerFqNames())
            safeAs<TypeAliasDescriptor>()?.run {
                result.addAll(extractDslMarkerFqNames(this.underlyingType))
            }
        }

        kotlinType.constructor.declarationDescriptor?.getAllSuperClassifiers()?.asIterable()
            ?.flatMapTo(result) { it.annotations.extractDslMarkerFqNames() }

        return result
    }

    val DSL_MARKER_FQ_NAME = FqName("kotlin.DslMarker")
}


private fun Annotations.extractDslMarkerFqNames() =
    filter(AnnotationDescriptor::isDslMarker).map { it.fqName!! }

private fun AnnotationDescriptor.isDslMarker(): Boolean {
    val classDescriptor = annotationClass ?: return false
    return classDescriptor.annotations.hasAnnotation(DslMarkerUtils.DSL_MARKER_FQ_NAME)
}
