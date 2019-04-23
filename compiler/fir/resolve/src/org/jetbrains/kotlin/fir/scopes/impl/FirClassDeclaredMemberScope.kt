/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirClassDeclaredMemberScope(
    klass: FirRegularClass
) : FirScope {
    private val classId = klass.symbol.classId
    private val callablesIndex by lazy {
        klass.declarations.filterIsInstance<FirCallableDeclaration>()
            .map { it.symbol }.groupBy { it.callableId }
    }
    private val classIndex by lazy {
        klass.declarations.filterIsInstance<FirRegularClass>()
            .map { it.symbol }.associateBy { it.classId }
    }


    override fun processFunctionsByName(name: Name, processor: (ConeFunctionSymbol) -> ProcessorAction): ProcessorAction {
        val matchedClass = classIndex[ClassId(classId.packageFqName, classId.relativeClassName.child(name), false)]

        if (matchedClass != null) {
            if (FirClassDeclaredMemberScope(matchedClass.fir).processFunctionsByName(name, processor) == STOP) {
                return STOP
            }
        }
        val symbols = callablesIndex[CallableId(classId.packageFqName, classId.relativeClassName, name)] ?: emptyList()
        for (symbol in symbols) {
            if (symbol is ConeFunctionSymbol && !processor(symbol)) {
                return STOP
            }
        }
        return NEXT
    }

    override fun processPropertiesByName(name: Name, processor: (ConeVariableSymbol) -> ProcessorAction): ProcessorAction {
        val symbols = callablesIndex[CallableId(classId.packageFqName, classId.relativeClassName, name)] ?: emptyList()
        for (symbol in symbols) {
            if (symbol is ConePropertySymbol && !processor(symbol)) {
                return STOP
            }
        }
        return NEXT
    }

    override fun processClassifiersByName(name: Name, position: FirPosition, processor: (ConeClassifierSymbol) -> Boolean): Boolean {
        val matchedClass = classIndex[classId.createNestedClassId(name)]
        if (matchedClass != null && !processor(matchedClass)) {
            return false
        }

        return super.processClassifiersByName(name, position, processor)
    }
}
