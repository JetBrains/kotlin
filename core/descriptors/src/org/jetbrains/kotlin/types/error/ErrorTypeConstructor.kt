/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.error

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeRefinement
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

class ErrorTypeConstructor(val kind: ErrorTypeKind, private vararg val formatParams: String) : TypeConstructor {
    private val debugText = ErrorEntity.ERROR_TYPE.debugText.format(kind.debugMessage.format(*formatParams))

    fun getParam(i: Int): String = formatParams[i]

    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()
    override fun getSupertypes(): Collection<KotlinType> = emptyList()
    override fun isFinal(): Boolean = false
    override fun isDenotable(): Boolean = false
    override fun getDeclarationDescriptor(): ClassifierDescriptor = ErrorUtils.errorClass
    override fun getBuiltIns(): KotlinBuiltIns = DefaultBuiltIns.Instance
    override fun toString(): String = debugText
    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): TypeConstructor = this
}
