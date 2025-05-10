/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.scopes.FirPlatformDeclarationFilter
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.name.StandardClassIds

internal object FirJvmPlatformDeclarationFilter {
    fun isFunctionAvailable(
        function: FirSimpleFunction,
        firJavaClassName: FirRegularClass,
        javaClassScope: FirTypeScope,
        session: FirSession,
    ): Boolean {
        if (FirPlatformDeclarationFilter.isNotPlatformDependent(function, session)) return true

        var isFunctionPresentInJavaAnalogue = false

        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.NewShapeForFirstLastFunctionsInKotlinList)
            && isFirstOrLastMethodInList(function, firJavaClassName)
        ) {
            return false
        }

        val javaRepresentationName = SpecialGenericSignatures.SIGNATURE_TO_JVM_REPRESENTATION_NAME[
            SignatureBuildingComponents.signature(
                firJavaClassName.classId,
                function.computeJvmDescriptor()
            )
        ] ?: function.name

        val jvmDescriptorOfKotlinFunction = function.computeJvmDescriptor(customName = javaRepresentationName.asString())
        javaClassScope.processFunctionsByName(javaRepresentationName) { javaAnalogueFunctionSymbol ->
            if (javaAnalogueFunctionSymbol.fir.computeJvmDescriptor() == jvmDescriptorOfKotlinFunction) {
                isFunctionPresentInJavaAnalogue = true
            }
        }
        return isFunctionPresentInJavaAnalogue
    }

    private fun isFirstOrLastMethodInList(
        function: FirSimpleFunction,
        containingClass: FirRegularClass,
    ): Boolean =
        containingClass.classId == StandardClassIds.List && (function.name.asString() == "first" || function.name.asString() == "last")
}
