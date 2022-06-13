/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.model.TypeVariableTypeConstructorMarker

class TypeVariableTypeConstructor(
    private val builtIns: KotlinBuiltIns,
    val debugName: String,
    val originalTypeParameter: TypeParameterDescriptor?
) : TypeConstructor, TypeVariableTypeConstructorMarker {
    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()
    override fun getSupertypes(): Collection<KotlinType> = emptyList()
    override fun isFinal(): Boolean = false
    override fun isDenotable(): Boolean = false
    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null

    override fun getBuiltIns() = builtIns

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): TypeConstructor = this

    override fun toString() = "TypeVariable($debugName)"

    var isContainedInInvariantOrContravariantPositions: Boolean = false
}
