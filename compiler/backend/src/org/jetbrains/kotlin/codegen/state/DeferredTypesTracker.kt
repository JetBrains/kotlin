/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.codegen.AnnotationCodegen
import org.jetbrains.kotlin.types.KotlinType

private const val DEFERRED_TYPE_CLASSNAME_PREFIX = "error/DeferredType"

interface DeferredTypesTracker {
    class TypeInfo(val jvmDescriptorOrGenericSignature: String, val nullabilityAnnotation: Class<*>?)

    /**
     * @return internal name to use as a stub for the deferred type
     */
    fun registerDeferredTypeComputation(
        kotlinType: KotlinType,
        deferredJvmTypeRepresentation: () -> String
    ): String

    /**
     * @param typeText text representation of a type (i.e., its source code representation)
     */
    fun getDeferredTypeInfoComputation(typeText: String): Function0<TypeInfo>?

    object Throwing : DeferredTypesTracker {
        override fun registerDeferredTypeComputation(
            kotlinType: KotlinType,
            deferredJvmTypeRepresentation: () -> String
        ) = throw IllegalStateException()

        override fun getDeferredTypeInfoComputation(typeText: String): Function0<TypeInfo>? = throw IllegalStateException()
    }
}

class DeferredTypesTrackerImpl : DeferredTypesTracker {
    private val typeInfoComputations: MutableMap<String, () -> DeferredTypesTracker.TypeInfo> = mutableMapOf()

    override fun registerDeferredTypeComputation(
        kotlinType: KotlinType,
        deferredJvmTypeRepresentation: () -> String
    ): String {
        val internalName = DEFERRED_TYPE_CLASSNAME_PREFIX + typeInfoComputations.size

        val computation by lazy(LazyThreadSafetyMode.PUBLICATION) {
            DeferredTypesTracker.TypeInfo(
                deferredJvmTypeRepresentation(),
                AnnotationCodegen.getNullabilityAnnotationFromType(kotlinType)
            )
        }

        typeInfoComputations[internalName] = { computation }
        return internalName
    }

    override fun getDeferredTypeInfoComputation(typeText: String): Function0<DeferredTypesTracker.TypeInfo>? =
        typeInfoComputations[typeText.replace('.', '/')]
}
