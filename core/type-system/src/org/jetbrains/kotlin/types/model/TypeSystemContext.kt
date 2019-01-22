/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.model

interface KotlinTypeIM
interface TypeArgumentIM
interface TypeConstructorIM
interface TypeParameterIM

interface SimpleTypeIM : KotlinTypeIM
interface CapturedTypeIM : SimpleTypeIM
interface DefinitelyNotNullTypeIM : SimpleTypeIM

interface FlexibleTypeIM : KotlinTypeIM
interface DynamicTypeIM : FlexibleTypeIM
interface RawTypeIM : FlexibleTypeIM


enum class TypeVariance {
    IN,
    OUT,
    INV
}


interface TypeSystemOptimizationContext {
    /**
     *  @return true is a.arguments == b.arguments, or false if not supported
     */
    fun identicalArguments(a: SimpleTypeIM, b: SimpleTypeIM) = false
}

interface TypeSystemContext : TypeSystemOptimizationContext {
    fun KotlinTypeIM.asSimpleType(): SimpleTypeIM?
    fun KotlinTypeIM.asFlexibleType(): FlexibleTypeIM?

    fun FlexibleTypeIM.asDynamicType(): DynamicTypeIM?
    fun FlexibleTypeIM.asRawType(): RawTypeIM?

    fun FlexibleTypeIM.upperBound(): SimpleTypeIM
    fun FlexibleTypeIM.lowerBound(): SimpleTypeIM

    fun SimpleTypeIM.asCapturedType(): CapturedTypeIM?
    fun SimpleTypeIM.asDefinitelyNotNullType(): DefinitelyNotNullTypeIM?
    fun SimpleTypeIM.isMarkedNullable(): Boolean
    fun SimpleTypeIM.typeConstructor(): TypeConstructorIM

    fun SimpleTypeIM.argumentsCount(): Int
    fun SimpleTypeIM.getArgument(index: Int): TypeArgumentIM

    fun TypeArgumentIM.isStarProjection(): Boolean
    fun TypeArgumentIM.getVariance(): TypeVariance
    fun TypeArgumentIM.getType(): KotlinTypeIM

    fun TypeConstructorIM.isErrorTypeConstructor(): Boolean
    fun TypeConstructorIM.parametersCount(): Int
    fun TypeConstructorIM.getParameter(index: Int): TypeParameterIM
    fun TypeConstructorIM.supertypesCount(): Int
    fun TypeConstructorIM.getSupertype(index: Int): KotlinTypeIM

    fun TypeParameterIM.getVariance(): TypeVariance
    fun TypeParameterIM.upperBoundCount(): Int
    fun TypeParameterIM.getUpperBound(index: Int): KotlinTypeIM
    fun TypeParameterIM.getTypeConstructor(): TypeConstructorIM

    fun isEqualTypeConstructors(c1: TypeConstructorIM, c2: TypeConstructorIM): Boolean
}