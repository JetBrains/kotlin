/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.isMaybeMainFunction
import org.jetbrains.kotlin.fir.java.findJvmNameValue
import org.jetbrains.kotlin.fir.java.findJvmStaticAnnotation
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName

/**
 * Find a single class that contains a valid main function
 * The main function validity is determined by the [isMaybeMainFunction] and some additional check:
 * the function should either be top-level or a member of a non-anonymous object
 * If many main functions are found in the same file then the one with parameters is "chosen", so we do not consider it a conflict.
 * Otherwise, if many main functions are found in one or several files, no one is chosen and the function returns "null"
 */
fun findMainClass(fir: List<FirFile>): FqName? {
    val groupedMainFunctions = mutableMapOf<FirDeclaration, MutableList<FirSimpleFunction>>()
    val visitor = FirMainClassFinder(groupedMainFunctions)
    fir.forEach { it.accept(visitor, it to null) }

    val singleGroup = groupedMainFunctions.asIterable().singleOrNull() ?: return null
    return when (val parent = singleGroup.key) {
        is FirFile -> {
            // if we have some parameterless mains and one main with parameters, it is considered valid
            if (singleGroup.value.size > 1 &&
                singleGroup.value.count { it.valueParameters.isNotEmpty() || it.receiverParameter != null } > 1
            ) {
                null
            } else {
                PackagePartClassUtils.getPackagePartFqName(parent.packageFqName, parent.name)
            }
        }
        is FirRegularClass -> {
            if (singleGroup.value.size > 1) null
            else parent.classId.asSingleFqName()
        }
        else -> null
    }
}

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
private class FirMainClassFinder(
    private var groupedMainFunctions: MutableMap<FirDeclaration, MutableList<FirSimpleFunction>>
) : FirVisitor<Unit, Pair<FirDeclaration, FirRegularClass?>>() {

    override fun visitElement(element: FirElement, parents: Pair<FirDeclaration, FirRegularClass?>) {}

    override fun visitFile(file: FirFile, parents: Pair<FirDeclaration, FirRegularClass?>) {
        file.acceptChildren(this, file to null)
    }

    override fun visitRegularClass(regularClass: FirRegularClass, parents: Pair<FirDeclaration, FirRegularClass?>) {
        if (!regularClass.isLocal) {
            regularClass.acceptChildren(this, regularClass to (if (regularClass.isCompanion) parents.first as? FirRegularClass else null))
        }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, parents: Pair<FirDeclaration, FirRegularClass?>) {

        if (!simpleFunction.isMaybeMainFunction(
                getPlatformName = { findJvmNameValue() },
                isPlatformStatic = { findJvmStaticAnnotation() != null },
            )
        ) return

        val (parent, grandparent) = parents
        if (parent is FirRegularClass && parent.classKind != ClassKind.OBJECT) return

        groupedMainFunctions.getOrPut(grandparent ?: parent, defaultValue = { mutableListOf() }).add(simpleFunction)
    }
}
