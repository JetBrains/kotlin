/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtTypeInfoProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.getFunctionTypeKind
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
        return JavaSingleAbstractMethodUtils.isSamType(type.fe10Type)
    }

    override fun getFunctionClassKind(type: KtType): FunctionTypeKind? {
        require(type is KtFe10Type)
        return type.fe10Type.constructor.declarationDescriptor?.getFunctionTypeKind()
    }

    override fun canBeNull(type: KtType): Boolean {
        require(type is KtFe10Type)
        return TypeUtils.isNullableType(type.fe10Type)
    }

    override fun isDenotable(type: KtType): Boolean {
        require(type is KtFe10Type)
        val kotlinType = type.fe10Type
        return kotlinType.isDenotable()
    }

    override fun isArrayOrPrimitiveArray(type: KtType): Boolean {
        require(type is KtFe10Type)
        return KotlinBuiltIns.isArrayOrPrimitiveArray(type.fe10Type)
    }

    override fun isNestedArray(type: KtType): Boolean {
        if (!isArrayOrPrimitiveArray(type)) return false
        require(type is KtFe10Type)
        val unwrappedType = type.fe10Type
        val elementType = unwrappedType.constructor.builtIns.getArrayElementType(unwrappedType)
        return KotlinBuiltIns.isArrayOrPrimitiveArray(elementType)
    }

    /** Expanded by default */
    override fun fullyExpandedType(type: KtType): KtType = type

    private fun KotlinType.isDenotable(): Boolean {
        if (this is DefinitelyNotNullType) return false
        return constructor.isDenotable &&
                constructor.declarationDescriptor?.name != SpecialNames.NO_NAME_PROVIDED &&
                arguments.all { it.type.isDenotable() }
    }
}
