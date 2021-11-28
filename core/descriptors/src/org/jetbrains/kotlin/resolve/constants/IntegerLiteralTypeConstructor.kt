/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.constants

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.TypeRefinement

class IntegerLiteralTypeConstructor : TypeConstructor {
    companion object {
        fun findCommonSuperType(types: Collection<SimpleType>): SimpleType? =
            findCommonSuperTypeOrIntersectionType(types, Companion.Mode.COMMON_SUPER_TYPE)

        fun findIntersectionType(types: Collection<SimpleType>): SimpleType? =
            findCommonSuperTypeOrIntersectionType(types, Companion.Mode.INTERSECTION_TYPE)

        private enum class Mode {
            COMMON_SUPER_TYPE, INTERSECTION_TYPE
        }

        /**
         * intersection(ILT(types), PrimitiveType) = commonSuperType(ILT(types), PrimitiveType) =
         *      PrimitiveType  in types  -> PrimitiveType
         *      PrimitiveType !in types -> null
         *
         * intersection(ILT(types_1), ILT(types_2)) = ILT(types_1 union types_2)
         *
         * commonSuperType(ILT(types_1), ILT(types_2)) = ILT(types_1 intersect types_2)
         */
        private fun findCommonSuperTypeOrIntersectionType(types: Collection<SimpleType>, mode: Mode): SimpleType? {
            if (types.isEmpty()) return null
            return types.reduce { left: SimpleType?, right: SimpleType? -> fold(left, right, mode) }
        }

        private fun fold(left: SimpleType?, right: SimpleType?, mode: Mode): SimpleType? {
            if (left == null || right == null) return null
            val leftConstructor = left.constructor
            val rightConstructor = right.constructor
            return when {
                leftConstructor is IntegerLiteralTypeConstructor && rightConstructor is IntegerLiteralTypeConstructor ->
                    fold(leftConstructor, rightConstructor, mode)

                leftConstructor is IntegerLiteralTypeConstructor -> fold(leftConstructor, right)
                rightConstructor is IntegerLiteralTypeConstructor -> fold(rightConstructor, left)
                else -> null
            }
        }

        private fun fold(left: IntegerLiteralTypeConstructor, right: IntegerLiteralTypeConstructor, mode: Mode): SimpleType? {
            val possibleTypes = when (mode) {
                Mode.COMMON_SUPER_TYPE -> left.possibleTypes intersect right.possibleTypes
                Mode.INTERSECTION_TYPE -> left.possibleTypes union right.possibleTypes
            }
            val constructor = IntegerLiteralTypeConstructor(left.value, left.module, possibleTypes)
            return KotlinTypeFactory.integerLiteralType(Annotations.EMPTY, constructor, false)
        }

        private fun fold(left: IntegerLiteralTypeConstructor, right: SimpleType): SimpleType? =
            if (right in left.possibleTypes) right else null

    }

    private val value: Long
    private val module: ModuleDescriptor
    val possibleTypes: Set<KotlinType>

    constructor(value: Long, module: ModuleDescriptor, parameters: CompileTimeConstant.Parameters) {
        this.value = value
        this.module = module

        val possibleTypes = mutableSetOf<KotlinType>()

        fun checkBoundsAndAddPossibleType(value: Long, kotlinType: KotlinType) {
            if (value in kotlinType.minValue()..kotlinType.maxValue()) {
                possibleTypes.add(kotlinType)
            }
        }

        fun addSignedPossibleTypes() {
            checkBoundsAndAddPossibleType(value, builtIns.intType)
            possibleTypes.add(builtIns.longType)
            checkBoundsAndAddPossibleType(value, builtIns.byteType)
            checkBoundsAndAddPossibleType(value, builtIns.shortType)
        }

        fun addUnsignedPossibleTypes() {
            checkBoundsAndAddPossibleType(value, module.uIntType)
            possibleTypes.add(module.uLongType)
            checkBoundsAndAddPossibleType(value, module.uByteType)
            checkBoundsAndAddPossibleType(value, module.uShortType)
        }

        val isUnsigned = parameters.isUnsignedNumberLiteral
        val isConvertable = parameters.isConvertableConstVal

        if (isUnsigned || isConvertable) {
            assert(hasUnsignedTypesInModuleDependencies(module)) {
                "Unsigned types should be on classpath to create an unsigned type constructor"
            }
        }

        when {
            isConvertable -> {
                addSignedPossibleTypes()
                addUnsignedPossibleTypes()
            }

            isUnsigned -> addUnsignedPossibleTypes()

            else -> addSignedPossibleTypes()
        }

        this.possibleTypes = possibleTypes
    }

    private constructor(value: Long, module: ModuleDescriptor, possibleTypes: Set<KotlinType>) {
        this.value = value
        this.module = module
        this.possibleTypes = possibleTypes
    }

    private val type = KotlinTypeFactory.integerLiteralType(Annotations.EMPTY, this, false)

    private fun isContainsOnlyUnsignedTypes(): Boolean = module.allSignedLiteralTypes.all { it !in possibleTypes }

    private val supertypes: List<KotlinType> by lazy {
        val result = mutableListOf(builtIns.comparable.defaultType.replace(listOf(TypeProjectionImpl(Variance.IN_VARIANCE, type))))
        if (!isContainsOnlyUnsignedTypes()) {
            result += builtIns.numberType
        }
        result
    }

    fun getApproximatedType(): KotlinType = when {
        builtIns.intType in possibleTypes -> builtIns.intType
        builtIns.longType in possibleTypes -> builtIns.longType
        builtIns.byteType in possibleTypes -> builtIns.byteType
        builtIns.shortType in possibleTypes -> builtIns.shortType

        module.uIntType in possibleTypes -> module.uIntType
        module.uLongType in possibleTypes -> module.uLongType
        module.uByteType in possibleTypes -> module.uByteType
        module.uShortType in possibleTypes -> module.uShortType

        else -> throw IllegalStateException()
    }


    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun getSupertypes(): Collection<KotlinType> = supertypes

    override fun isFinal(): Boolean = true

    override fun isDenotable(): Boolean = false

    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null

    override fun getBuiltIns(): KotlinBuiltIns = module.builtIns

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): TypeConstructor = this

    override fun toString(): String {
        return "IntegerLiteralType${valueToString()}"
    }

    fun checkConstructor(constructor: TypeConstructor): Boolean = possibleTypes.any { it.constructor == constructor }

    private fun valueToString(): String = "[${possibleTypes.joinToString(",") { it.toString() }}]"

}
