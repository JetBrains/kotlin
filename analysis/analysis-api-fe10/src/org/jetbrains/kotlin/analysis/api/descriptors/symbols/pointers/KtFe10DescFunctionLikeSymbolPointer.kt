/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolPointerSignature
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class KtFe10DescFunctionLikeSymbolPointer<T : KtFunctionLikeSymbol>(
    private val callableId: CallableId,
    private val signature: String
) : KtSymbolPointer<T>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): T? {
        check(analysisSession is KtFe10AnalysisSession)
        val analysisContext = analysisSession.analysisContext

        val className = callableId.className
        val memberScope = if (className != null) {
            val outerClassId = ClassId(callableId.packageName, className, isLocal = false)
            analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(outerClassId)
                ?.unsubstitutedMemberScope
                ?: MemberScope.Empty
        } else {
            analysisContext.resolveSession.moduleDescriptor.getPackage(callableId.packageName).memberScope
        }

        @Suppress("UNCHECKED_CAST")
        return memberScope
            .getContributedDescriptors(DescriptorKindFilter.CALLABLES) { it == callableId.callableName }
            .filterIsInstance<CallableMemberDescriptor>()
            .firstOrNull { it.getSymbolPointerSignature() == signature }
            ?.toKtCallableSymbol(analysisContext) as? T
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFe10DescFunctionLikeSymbolPointer &&
            other.callableId == callableId &&
            other.signature == signature
}
