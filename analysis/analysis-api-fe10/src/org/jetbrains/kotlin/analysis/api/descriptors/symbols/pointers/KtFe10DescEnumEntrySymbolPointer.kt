/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class KtFe10DescEnumEntrySymbolPointer(private val classId: ClassId, private val entryName: Name) : KtSymbolPointer<KtEnumEntrySymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtEnumEntrySymbol? {
        check(analysisSession is KtFe10AnalysisSession)
        val analysisContext = analysisSession.analysisContext

        val entryDescriptor = analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(classId)
            ?.unsubstitutedMemberScope
            ?.getContributedClassifier(entryName, NoLookupLocation.FROM_IDE)

        if (entryDescriptor is ClassDescriptor && entryDescriptor.kind == ClassKind.ENUM_ENTRY) {
            return KtFe10DescEnumEntrySymbol(entryDescriptor, analysisContext)
        }

        return null
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFe10DescEnumEntrySymbolPointer &&
            other.classId == classId &&
            other.entryName == entryName
}
