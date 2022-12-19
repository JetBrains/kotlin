/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.load.java.sam.JvmSamConversionOracle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.sam.createSamConstructorFunction
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull

class KtFe10DescSamConstructorSymbolPointer(private val classId: ClassId) : KtSymbolPointer<KtSamConstructorSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtSamConstructorSymbol? {
        check(analysisSession is KtFe10AnalysisSession)
        val analysisContext = analysisSession.analysisContext

        val samInterface = analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(classId)
        if (samInterface == null || getSingleAbstractMethodOrNull(samInterface) == null) {
            return null
        }

        val constructorDescriptor = createSamConstructorFunction(
            samInterface.containingDeclaration,
            samInterface,
            analysisContext.resolveSession.samConversionResolver,
            JvmSamConversionOracle(analysisContext.resolveSession.languageVersionSettings)
        )

        return KtFe10DescSamConstructorSymbol(constructorDescriptor, analysisContext)
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFe10DescSamConstructorSymbolPointer &&
            other.classId == classId
}
