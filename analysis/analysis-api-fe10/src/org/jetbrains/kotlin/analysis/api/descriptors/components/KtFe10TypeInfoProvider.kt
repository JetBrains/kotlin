/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtTypeInfoProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.builtins.getFunctionalClassKind
import org.jetbrains.kotlin.load.java.sam.JavaSingleAbstractMethodUtils
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.DefinitelyNotNullType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

internal class KtFe10TypeInfoProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtTypeInfoProvider(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun isFunctionalInterfaceType(type: KtType): Boolean {
        require(type is KtFe10Type)
        return JavaSingleAbstractMethodUtils.isSamType(type.type)
    }

    override fun getFunctionClassKind(type: KtType): FunctionClassKind? {
        require(type is KtFe10Type)
        return type.type.constructor.declarationDescriptor?.getFunctionalClassKind()
    }

    override fun canBeNull(type: KtType): Boolean {
        require(type is KtFe10Type)
        return TypeUtils.isNullableType(type.type)
    }

    override fun isDenotable(type: KtType): Boolean {
        require(type is KtFe10Type)
        val kotlinType = type.type
        return kotlinType.isDenotable()
    }

    private fun KotlinType.isDenotable(): Boolean {
        if (this is DefinitelyNotNullType) return false
        return constructor.isDenotable &&
                constructor.declarationDescriptor?.name != SpecialNames.NO_NAME_PROVIDED &&
                arguments.all { it.type.isDenotable() }
    }
}
