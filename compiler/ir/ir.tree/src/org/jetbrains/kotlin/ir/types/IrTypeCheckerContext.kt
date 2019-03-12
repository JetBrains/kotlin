/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.types.impl.makeTypeIntersection
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

class IrTypeCheckerContext(override val irBuiltIns: IrBuiltIns) : IrTypeSystemContext, AbstractTypeCheckerContext() {

    override fun substitutionSupertypePolicy(type: SimpleTypeMarker): SupertypesPolicy.DoCustomTransform {
        return object : SupertypesPolicy.DoCustomTransform() {
            override fun transformType(context: AbstractTypeCheckerContext, type: KotlinTypeMarker): SimpleTypeMarker {
                return makeTypeProjection(type as IrType, Variance.INVARIANT) as SimpleTypeMarker
            }
        }
    }

    override fun areEqualTypeConstructors(a: TypeConstructorMarker, b: TypeConstructorMarker) = super.isEqualTypeConstructors(a, b)

    @Suppress("UNCHECKED_CAST")
    override fun intersectTypes(types: List<KotlinTypeMarker>) = makeTypeIntersection(types as List<IrType>)

    override val isErrorTypeEqualsToAnything = false
    override val KotlinTypeMarker.isAllowedTypeVariable: Boolean
        get() = false

}