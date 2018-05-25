/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

private const val DEFERRED_TYPE_CLASSNAME_PREFIX = "error/DeferredType"

interface DeferredTypesTracker {
    /**
     * @return internal name to use as a stub for the deferred type
     */
    fun registerDeferredTypeComputation(
        deferredJvmTypeRepresentation: () -> String
    ): String

    /**
     * @param typeText text representation of a type (i.e., its source code representation)
     */
    fun getDeferredTypeComputation(typeText: String): Function0<String>?

    object Throwing : DeferredTypesTracker {
        override fun registerDeferredTypeComputation(deferredJvmTypeRepresentation: () -> String) = throw IllegalStateException()
        override fun getDeferredTypeComputation(typeText: String): Function0<String>? = throw IllegalStateException()
    }
}

class DeferredTypesTrackerImpl : DeferredTypesTracker {
    private val deferredTypesMap: MutableMap<String, () -> String> = mutableMapOf()

    override fun registerDeferredTypeComputation(
        deferredJvmTypeRepresentation: () -> String
    ): String {
        val internalName = DEFERRED_TYPE_CLASSNAME_PREFIX + deferredTypesMap.size
        deferredTypesMap[internalName] = deferredJvmTypeRepresentation
        return internalName
    }

    override fun getDeferredTypeComputation(typeText: String): Function0<String>? =
        deferredTypesMap[typeText.replace('.', '/')]
}
