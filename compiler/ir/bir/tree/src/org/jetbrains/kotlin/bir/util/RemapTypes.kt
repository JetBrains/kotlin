/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.utils.memoryOptimizedMap

fun BirElement.remapTypes(typeRemapper: BirTypeParameterRemapper) {
    accept { element ->
        if (element is BirClass) {
            element.superTypes = element.superTypes.memoryOptimizedMap { typeRemapper.remapType(it) }
        }
        if (element is BirValueParameter) {
            element.type = typeRemapper.remapType(element.type)
            element.varargElementType = element.varargElementType?.let { typeRemapper.remapType(it) }
        }
        if (element is BirTypeParameter) {
            element.superTypes = element.superTypes.memoryOptimizedMap { typeRemapper.remapType(it) }
        }
        if (element is BirVariable) {
            element.type = typeRemapper.remapType(element.type)
        }
        if (element is BirFunction) {
            element.returnType = typeRemapper.remapType(element.returnType)
        }
        if (element is BirField) {
            element.type = typeRemapper.remapType(element.type)
        }
        if (element is BirLocalDelegatedProperty) {
            element.type = typeRemapper.remapType(element.type)
        }
        if (element is BirTypeAlias) {
            element.expandedType = typeRemapper.remapType(element.expandedType)
        }
        if (element is BirExpression) {
            element.type = typeRemapper.remapType(element.type)
        }
        if (element is BirMemberAccessExpression<*>) {
            element.typeArguments = element.typeArguments.memoryOptimizedMap { it?.let { typeRemapper.remapType(it) } }
        }
        if (element is BirTypeOperatorCall) {
            element.typeOperand = typeRemapper.remapType(element.typeOperand)
        }
        if (element is BirVararg) {
            element.varargElementType = typeRemapper.remapType(element.varargElementType)
        }
        if (element is BirClassReference) {
            element.classType = typeRemapper.remapType(element.classType)
        }

        element.walkIntoChildren()
    }
}