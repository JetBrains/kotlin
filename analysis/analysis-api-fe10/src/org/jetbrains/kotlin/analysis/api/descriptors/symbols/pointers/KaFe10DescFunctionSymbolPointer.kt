/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolPointerSignature
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseCachedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope

internal class KaFe10DescFunctionSymbolPointer<T : KaFunctionSymbol>(
    private val callableId: CallableId,
    private val signature: String,
    originalSymbol: T?
) : KaBaseCachedSymbolPointer<T>(originalSymbol) {
    @KaImplementationDetail
    override fun restoreIfNotCached(analysisSession: KaSession): T? {
        check(analysisSession is KaFe10Session)
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

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFe10DescFunctionSymbolPointer &&
            other.callableId == callableId &&
            other.signature == signature
}
