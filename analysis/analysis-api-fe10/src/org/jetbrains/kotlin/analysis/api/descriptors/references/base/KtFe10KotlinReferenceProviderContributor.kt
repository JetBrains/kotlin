/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.references.base

import org.jetbrains.kotlin.analysis.api.descriptors.references.*
import org.jetbrains.kotlin.idea.references.KotlinPsiReferenceRegistrar
import org.jetbrains.kotlin.idea.references.KotlinReferenceProviderContributor

class KtFe10KotlinReferenceProviderContributor : KotlinReferenceProviderContributor {
    override fun registerReferenceProviders(registrar: KotlinPsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(factory = ::KtFe10SimpleNameReference)
            registerProvider(factory = ::KtFe10ForLoopInReference)
            registerProvider(factory = ::KtFe10InvokeFunctionReference)
            registerProvider(factory = ::KtFe10PropertyDelegationMethodsReference)
            registerProvider(factory = ::KtFe10DestructuringDeclarationEntry)
            registerProvider(factory = ::KtFe10ArrayAccessReference)
            registerProvider(factory = ::KtFe10ConstructorDelegationReference)
            registerProvider(factory = ::KtFe10CollectionLiteralReference)
        }
    }
}