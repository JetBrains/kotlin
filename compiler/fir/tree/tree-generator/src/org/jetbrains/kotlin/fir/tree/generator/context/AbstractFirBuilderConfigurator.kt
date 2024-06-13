/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.context

import org.jetbrains.kotlin.fir.tree.generator.Model
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation
import org.jetbrains.kotlin.generators.tree.config.AbstractBuilderConfigurator

abstract class AbstractFirBuilderConfigurator<T : AbstractFirTreeBuilder>(model: Model) :
    AbstractBuilderConfigurator<Element, Implementation, Field>(model) {

    final override val namePrefix: String
        get() = "Fir"

    final override val defaultBuilderPackage: String
        get() = "org.jetbrains.kotlin.fir.tree.builder"

    protected fun BuilderConfigurationContext.defaultNoReceivers(notNullExplicitReceiver: Boolean = false) {
        if (!notNullExplicitReceiver) {
            defaultNull("explicitReceiver")
        }
        defaultNull("dispatchReceiver", "extensionReceiver")
    }
}
