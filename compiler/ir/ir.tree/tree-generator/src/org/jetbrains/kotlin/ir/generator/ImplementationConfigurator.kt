/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.StandardTypes
import org.jetbrains.kotlin.ir.generator.config.AbstractIrTreeImplementationConfigurator
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.ListField

object ImplementationConfigurator : AbstractIrTreeImplementationConfigurator() {
    override fun configure(model: Model): Unit = with(IrTree) {
        impl(anonymousInitializer) {
            implementation.doPrint = false
        }

        impl(simpleFunction) {
            implementation.doPrint = false
        }
        impl(functionWithLateBinding) {
            implementation.doPrint = false
        }

        impl(constructor) {
            implementation.doPrint = false
        }

        impl(field) {
            implementation.doPrint = false
        }

        impl(property) {
            implementation.doPrint = false
        }
        impl(propertyWithLateBinding) {
            implementation.doPrint = false
        }

        impl(localDelegatedProperty) {
            implementation.doPrint = false
        }

        impl(typeParameter) {
            implementation.doPrint = false
        }

        impl(valueParameter) {
            implementation.doPrint = false
        }

        impl(variable) {
            implementation.doPrint = false
        }

        impl(`class`) {
            implementation.doPrint = false
        }

        impl(enumEntry) {
            implementation.doPrint = false
        }

        impl(script) {
            implementation.doPrint = false
        }

        impl(moduleFragment) {
            implementation.doPrint = false
        }

        impl(errorDeclaration) {
            implementation.doPrint = false
        }

        impl(externalPackageFragment) {
            implementation.doPrint = false
        }

        impl(file) {
            implementation.doPrint = false
        }

        impl(typeAlias) {
            implementation.doPrint = false
        }
    }

    override fun configureAllImplementations(model: Model) {
        configureFieldInAllImplementations("parent") {
            isLateinit("parent")
            isMutable("parent")
        }

        configureFieldInAllImplementations("attributeOwnerId") {
            default(it, "this")
        }
        configureFieldInAllImplementations("originalBeforeInline") {
            defaultNull(it)
        }

        configureFieldInAllImplementations("metadata") {
            defaultNull(it)
        }

        configureFieldInAllImplementations("annotations") {
            defaultEmptyList(it)
        }

        configureFieldInAllImplementations("overriddenSymbols") {
            defaultEmptyList(it)
        }

        configureFieldInAllImplementations("typeParameters") {
            defaultEmptyList(it)
        }

        configureFieldInAllImplementations("statements") {
            default(it, "ArrayList(2)")
        }

        configureFieldInAllImplementations("descriptor", { impl -> impl.allFields.any { it.name == "symbol" } }) {
            default(it, "symbol.descriptor", withGetter = true)
        }

        configureFieldInAllImplementations(
            fieldName = null,
            fieldPredicate = { it is ListField && it.isChild && it.listType == StandardTypes.mutableList }
        ) {
            default(it, "ArrayList()")
        }

        // Generation of implementation classes of IrExpression are left out for subsequent MR, as a part of KT-65773.
        for (element in model.elements) {
            if (element.category == Element.Category.Expression) {
                for (implementation in element.implementations) {
                    implementation.doPrint = false
                }
            }
        }
    }
}