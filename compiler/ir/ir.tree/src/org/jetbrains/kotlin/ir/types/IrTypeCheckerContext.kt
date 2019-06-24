/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeArgumentMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

class IrTypeCheckerContext(override val irBuiltIns: IrBuiltIns) : IrTypeSystemContext, AbstractTypeCheckerContext() {

    override fun anyType(): SimpleTypeMarker =
        irBuiltIns.anyType as IrSimpleType

    override fun substitutionSupertypePolicy(type: SimpleTypeMarker): SupertypesPolicy.DoCustomTransform {
        require(type is IrSimpleType)
        val parameters = extractTypeParameters((type.classifier as IrClassSymbol).owner).map { it.symbol }
        val substitution = parameters.zip(type.arguments).toMap()
        return object : SupertypesPolicy.DoCustomTransform() {
            override fun transformType(context: AbstractTypeCheckerContext, type: KotlinTypeMarker): SimpleTypeMarker {
                require(type is IrSimpleType)

                return substituteArguments(type)
            }

            private fun substituteArguments(type: IrSimpleType): IrSimpleType {
                val realArguments = type.arguments.map {
                    substitute(it)
                }.toList()

                return IrSimpleTypeImpl(type.classifier, type.hasQuestionMark, realArguments, type.annotations)
            }

            private fun substitute(type: IrTypeArgument): IrTypeArgument {
                if (type is IrStarProjection) return type

                val actualType = (type as IrTypeProjection).type as IrSimpleType
                substitution[actualType.classifier]?.let { return it }
                return makeTypeProjection(substituteArguments(actualType), type.variance)
            }
        }
    }

    override fun areEqualTypeConstructors(a: TypeConstructorMarker, b: TypeConstructorMarker) = super.isEqualTypeConstructors(a, b)


    override val isErrorTypeEqualsToAnything = false
    override val KotlinTypeMarker.isAllowedTypeVariable: Boolean
        get() = false


    override fun newBaseTypeCheckerContext(errorTypesEqualToAnything: Boolean): AbstractTypeCheckerContext {
        return IrTypeCheckerContext(irBuiltIns)
    }

    override fun KotlinTypeMarker.removeExactAnnotation(): KotlinTypeMarker {
        // TODO remove 'Exact' annotation only
        return removeAnnotations()
    }

    override fun KotlinTypeMarker.isUninferredParameter(): Boolean = false

    override fun captureFromExpression(type: KotlinTypeMarker): KotlinTypeMarker? =
        error("Captured type is unsupported in IR")

    override fun SimpleTypeMarker.isPrimitiveType(): Boolean {
        // TODO this is currently used in overload resolution only
        return false
    }

    override fun KotlinTypeMarker.argumentsCount(): Int =
        when (this) {
            is IrSimpleType -> arguments.size
            else -> 0
        }

    override fun KotlinTypeMarker.getArgument(index: Int): TypeArgumentMarker =
        when (this) {
            is IrSimpleType -> arguments[index]
            else -> error("Type $this has no arguments")
        }

    override fun KotlinTypeMarker.mayBeTypeVariable(): Boolean {
        require(this is IrType)
        return false
    }
}