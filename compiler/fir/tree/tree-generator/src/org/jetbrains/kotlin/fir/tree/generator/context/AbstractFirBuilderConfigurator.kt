/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.context

import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.fir.tree.generator.model.FieldWithDefault
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation
import org.jetbrains.kotlin.generators.tree.AbstractBuilderConfigurator

abstract class AbstractFirBuilderConfigurator<T : AbstractFirTreeBuilder>(
    elements: List<Element>
) : AbstractBuilderConfigurator<Element, Implementation, FieldWithDefault, Field>(elements) {

    override val namePrefix: String
        get() = "Fir"

    override val defaultBuilderPackage: String
        get() = "org.jetbrains.kotlin.fir.tree.builder"

    override fun builderFieldFromElementField(elementField: Field) = FieldWithDefault(elementField.copy())
}
