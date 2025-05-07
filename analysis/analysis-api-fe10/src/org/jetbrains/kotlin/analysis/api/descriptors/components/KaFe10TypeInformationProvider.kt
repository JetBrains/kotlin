/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.getFunctionTypeKind
import org.jetbrains.kotlin.load.java.sam.JavaSingleAbstractMethodUtils
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.DefinitelyNotNullType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

internal class KaFe10TypeInformationProvider(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaBaseSessionComponent<KaFe10Session>(), KaTypeInformationProvider, KaFe10SessionComponent {
    override val KaType.isFunctionalInterface: Boolean
        get() = withValidityAssertion {
            require(this is KaFe10Type)
            return JavaSingleAbstractMethodUtils.isSamType(fe10Type)
        }

    override val KaType.functionTypeKind: FunctionTypeKind?
        get() = withValidityAssertion {
            require(this is KaFe10Type)
            return fe10Type.constructor.declarationDescriptor?.getFunctionTypeKind()
        }

    override val KaType.isNullable: Boolean
        get() = withValidityAssertion {
            require(this is KaFe10Type)
            return TypeUtils.isNullableType(fe10Type)
        }

    override val KaType.isMarkedNullable: Boolean
        get() = withValidityAssertion {
            return (this as KaFe10Type).fe10Type.isMarkedNullable
        }

    override val KaType.isDenotable: Boolean
        get() = withValidityAssertion {
            require(this is KaFe10Type)
            return fe10Type.isDenotable()
        }

    override val KaType.isArrayOrPrimitiveArray: Boolean
        get() = withValidityAssertion {
            require(this is KaFe10Type)
            return KotlinBuiltIns.isArrayOrPrimitiveArray(fe10Type)
        }

    override val KaType.isNestedArray: Boolean
        get() = withValidityAssertion {
            if (!isArrayOrPrimitiveArray) return false
            require(this is KaFe10Type)
            val unwrappedType = fe10Type
            val elementType = unwrappedType.constructor.builtIns.getArrayElementType(unwrappedType)
            return KotlinBuiltIns.isArrayOrPrimitiveArray(elementType)
        }

    /** Expanded by default */
    override val KaType.fullyExpandedType: KaType
        get() = withValidityAssertion { this }

    private fun KotlinType.isDenotable(): Boolean {
        if (this is DefinitelyNotNullType) return false
        return constructor.isDenotable &&
                constructor.declarationDescriptor?.name != SpecialNames.NO_NAME_PROVIDED &&
                arguments.all { it.type.isDenotable() }
    }
}
