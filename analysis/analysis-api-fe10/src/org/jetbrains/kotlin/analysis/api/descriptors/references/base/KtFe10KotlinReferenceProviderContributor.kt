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
            registerProvider(factory = ::CliKtFe10SimpleNameReference)
            registerProvider(factory = ::CliKtFe10ForLoopInReference)
            registerProvider(factory = ::CliKtFe10InvokeFunctionReference)
            registerProvider(factory = ::CliKtFe10PropertyDelegationMethodsReference)
            registerProvider(factory = ::CliKtFe10DestructuringDeclarationEntry)
            registerProvider(factory = ::CliKtFe10ArrayAccessReference)
            registerProvider(factory = ::CliKtFe10ConstructorDelegationReference)
            registerProvider(factory = ::CliKtFe10CollectionLiteralReference)
        }
    }
}