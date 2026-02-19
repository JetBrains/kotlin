/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

const val CHAR_RANGE_FQN = "kotlin.ranges.CharRange"
const val INT_RANGE_FQN = "kotlin.ranges.IntRange"
const val LONG_RANGE_FQN = "kotlin.ranges.LongRange"

const val CHAR_PROGRESSION_FQN = "kotlin.ranges.CharProgression"
const val INT_PROGRESSION_FQN = "kotlin.ranges.IntProgression"
const val LONG_PROGRESSION_FQN = "kotlin.ranges.LongProgression"

private const val CLOSED_FLOAT_RANGE_FQN = "kotlin.ranges.ClosedFloatRange"
private const val CLOSED_DOUBLE_RANGE_FQN = "kotlin.ranges.ClosedDoubleRange"
private const val CLOSED_RANGE_FQN = "kotlin.ranges.ClosedRange"
private const val CLOSED_FLOATING_POINT_RANGE_FQN = "kotlin.ranges.ClosedFloatingPointRange"
private const val COMPARABLE_RANGE_FQN = "kotlin.ranges.ComparableRange"

internal const val UINT_RANGE_FQN = "kotlin.ranges.UIntRange"
internal const val ULONG_RANGE_FQN = "kotlin.ranges.ULongRange"

internal const val UINT_PROGRESSION_FQN = "kotlin.ranges.UIntProgression"
internal const val ULONG_PROGRESSION_FQN = "kotlin.ranges.ULongProgression"

private val ALL_PROGRESSION_AND_RANGES = listOf(
    CHAR_RANGE_FQN, CHAR_PROGRESSION_FQN,
    INT_RANGE_FQN, INT_PROGRESSION_FQN,
    LONG_RANGE_FQN, LONG_PROGRESSION_FQN,
    CLOSED_FLOAT_RANGE_FQN, CLOSED_DOUBLE_RANGE_FQN,
    CLOSED_RANGE_FQN, CLOSED_FLOATING_POINT_RANGE_FQN,
    COMPARABLE_RANGE_FQN,
    UINT_RANGE_FQN, UINT_PROGRESSION_FQN,
    ULONG_RANGE_FQN, ULONG_PROGRESSION_FQN
)

fun getRangeOrProgressionElementType(rangeType: KotlinType, progressionsAndRanges: List<String> = ALL_PROGRESSION_AND_RANGES): KotlinType? {
    val rangeClassDescriptor = rangeType.constructor.declarationDescriptor as? ClassDescriptor ?: return null
    val builtIns = rangeClassDescriptor.builtIns
    val fqName = rangeClassDescriptor.fqNameSafe.asString()

    if (fqName !in progressionsAndRanges) return null

    return when (fqName) {
        CHAR_RANGE_FQN, CHAR_PROGRESSION_FQN -> builtIns.charType
        INT_RANGE_FQN, INT_PROGRESSION_FQN -> builtIns.intType
        LONG_RANGE_FQN, LONG_PROGRESSION_FQN -> builtIns.longType

        CLOSED_FLOAT_RANGE_FQN -> builtIns.floatType
        CLOSED_DOUBLE_RANGE_FQN -> builtIns.doubleType

        CLOSED_RANGE_FQN -> rangeType.arguments.singleOrNull()?.type
        CLOSED_FLOATING_POINT_RANGE_FQN -> rangeType.arguments.singleOrNull()?.type
        COMPARABLE_RANGE_FQN -> rangeType.arguments.singleOrNull()?.type

        UINT_RANGE_FQN, UINT_PROGRESSION_FQN ->
            rangeClassDescriptor.findTypeInModuleByTopLevelClassFqName(StandardNames.FqNames.uIntFqName)

        ULONG_RANGE_FQN, ULONG_PROGRESSION_FQN ->
            rangeClassDescriptor.findTypeInModuleByTopLevelClassFqName(StandardNames.FqNames.uLongFqName)

        else -> null
    }
}

private fun DeclarationDescriptor.findTypeInModuleByTopLevelClassFqName(fqName: FqName) =
    module.findClassAcrossModuleDependencies(ClassId.topLevel(fqName))?.defaultType
