/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.utils.keysToMap

class LocalClassesNavigationInfo(
    val parentForClass: Map<FirClass<*>, FirClass<*>?>,
    private val parentClassForFunction: Map<FirCallableMemberDeclaration<*>, FirClass<*>>,
    val allMembers: List<FirSymbolOwner<*>>
) {
    val designationMap by lazy {
        parentClassForFunction.keys.keysToMap {
            pathForCallable(it)
        }
    }

    private fun pathForCallable(callableMemberDeclaration: FirCallableMemberDeclaration<*>): List<FirClass<*>> {
        val result = mutableListOf<FirClass<*>>()
        var current = parentClassForFunction[callableMemberDeclaration]

        while (current != null) {
            result += current
            current = parentForClass[current]
        }

        return result.asReversed()
    }
}

fun FirClass<*>.collectLocalClassesNavigationInfo(): LocalClassesNavigationInfo =
    NavigationInfoVisitor().run {
        this@collectLocalClassesNavigationInfo.accept(this@run)

        LocalClassesNavigationInfo(parentForClass, resultingMap, allMembers)
    }

private class NavigationInfoVisitor : FirDefaultVisitorVoid() {
    val resultingMap = mutableMapOf<FirCallableMemberDeclaration<*>, FirClass<*>>()
    val parentForClass = mutableMapOf<FirClass<*>, FirClass<*>?>()
    val allMembers = mutableListOf<FirSymbolOwner<*>>()
    private var currentPath = persistentListOf<FirClass<*>>()

    override fun visitElement(element: FirElement) {}

    override fun visitRegularClass(regularClass: FirRegularClass) {
        visitClass(regularClass)
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) {
        visitClass(anonymousObject)
    }

    override fun <F : FirClass<F>> visitClass(klass: FirClass<F>) {
        parentForClass[klass] = currentPath.lastOrNull()
        val prev = currentPath
        currentPath = currentPath.add(klass)

        klass.acceptChildren(this)

        currentPath = prev
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
        visitCallableMemberDeclaration(simpleFunction)
    }

    override fun visitProperty(property: FirProperty) {
        visitCallableMemberDeclaration(property)
    }

    override fun visitConstructor(constructor: FirConstructor) {
        visitCallableMemberDeclaration(constructor)
    }

    override fun <F : FirCallableMemberDeclaration<F>> visitCallableMemberDeclaration(
        callableMemberDeclaration: FirCallableMemberDeclaration<F>
    ) {
        allMembers += callableMemberDeclaration
        if (callableMemberDeclaration.returnTypeRef !is FirImplicitTypeRef) return
        resultingMap[callableMemberDeclaration] = currentPath.last()
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {
        allMembers += anonymousInitializer
    }
}
