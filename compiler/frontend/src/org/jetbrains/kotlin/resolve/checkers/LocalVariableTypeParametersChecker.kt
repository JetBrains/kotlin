/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty

class LocalVariableTypeParametersChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtProperty || descriptor !is LocalVariableDescriptor) return

        val typeParameters = declaration.typeParameters
        val typeParametersList = declaration.typeParameterList
        if (typeParameters.isEmpty() || typeParametersList == null) return

        val diagnostic =
            if (context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitTypeParametersForLocalVariables))
                Errors.LOCAL_VARIABLE_WITH_TYPE_PARAMETERS
            else
                Errors.LOCAL_VARIABLE_WITH_TYPE_PARAMETERS_WARNING

        context.trace.report(diagnostic.on(typeParametersList))
    }
}
