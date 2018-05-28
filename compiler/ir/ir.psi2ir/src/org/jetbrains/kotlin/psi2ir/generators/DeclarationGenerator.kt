/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrErrorDeclarationImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeAliasImpl
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.endOffsetOrUndefined
import org.jetbrains.kotlin.psi2ir.startOffsetOrUndefined
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.types.KotlinType

class DeclarationGenerator(override val context: GeneratorContext) : Generator {

    private val typeTranslator = TypeTranslator(context.moduleDescriptor, context.symbolTable)

    fun KotlinType.toIrType() = typeTranslator.translateType(this)

    fun generateMemberDeclaration(ktDeclaration: KtDeclaration): IrDeclaration =
        when (ktDeclaration) {
            is KtNamedFunction ->
                FunctionGenerator(this).generateFunctionDeclaration(ktDeclaration)
            is KtProperty ->
                PropertyGenerator(this).generatePropertyDeclaration(ktDeclaration)
            is KtClassOrObject ->
                generateClassOrObjectDeclaration(ktDeclaration)
            is KtTypeAlias ->
                generateTypeAliasDeclaration(ktDeclaration)
            else ->
                IrErrorDeclarationImpl(
                    ktDeclaration.startOffset, ktDeclaration.endOffset,
                    getOrFail(BindingContext.DECLARATION_TO_DESCRIPTOR, ktDeclaration)
                )
        }

    fun generateClassMemberDeclaration(ktDeclaration: KtDeclaration, classDescriptor: ClassDescriptor): IrDeclaration =
        when (ktDeclaration) {
            is KtAnonymousInitializer ->
                AnonymousInitializerGenerator(this).generateAnonymousInitializerDeclaration(ktDeclaration, classDescriptor)
            is KtSecondaryConstructor ->
                FunctionGenerator(this).generateSecondaryConstructor(ktDeclaration)
            is KtEnumEntry ->
                generateEnumEntryDeclaration(ktDeclaration)
            else ->
                generateMemberDeclaration(ktDeclaration)
        }

    private fun generateEnumEntryDeclaration(ktEnumEntry: KtEnumEntry): IrEnumEntry =
        ClassGenerator(this).generateEnumEntry(ktEnumEntry)

    fun generateClassOrObjectDeclaration(ktClassOrObject: KtClassOrObject): IrClass =
        ClassGenerator(this).generateClass(ktClassOrObject)

    private fun generateTypeAliasDeclaration(ktDeclaration: KtTypeAlias): IrDeclaration =
        IrTypeAliasImpl(
            ktDeclaration.startOffset, ktDeclaration.endOffset, IrDeclarationOrigin.DEFINED,
            getOrFail(BindingContext.TYPE_ALIAS, ktDeclaration)
        )


    fun generateGlobalTypeParametersDeclarations(
        irTypeParametersOwner: IrTypeParametersContainer,
        from: List<TypeParameterDescriptor>
    ) {
        generateTypeParameterDeclarations(irTypeParametersOwner, from) { startOffset, endOffset, typeParameterDescriptor ->
            context.symbolTable.declareGlobalTypeParameter(
                startOffset,
                endOffset,
                IrDeclarationOrigin.DEFINED,
                typeParameterDescriptor
            )
        }
    }

    fun generateScopedTypeParameterDeclarations(
        irTypeParametersOwner: IrTypeParametersContainer,
        from: List<TypeParameterDescriptor>
    ) {
        generateTypeParameterDeclarations(irTypeParametersOwner, from) { startOffset, endOffset, typeParameterDescriptor ->
            context.symbolTable.declareScopedTypeParameter(
                startOffset,
                endOffset,
                IrDeclarationOrigin.DEFINED,
                typeParameterDescriptor
            )
        }
    }

    private fun generateTypeParameterDeclarations(
        irTypeParametersOwner: IrTypeParametersContainer,
        from: List<TypeParameterDescriptor>,
        declareTypeParameter: (Int, Int, TypeParameterDescriptor) -> IrTypeParameter
    ) {
        from.mapTo(irTypeParametersOwner.typeParameters) { typeParameterDescriptor ->
            val ktTypeParameterDeclaration = DescriptorToSourceUtils.getSourceFromDescriptor(typeParameterDescriptor)
            val startOffset = ktTypeParameterDeclaration.startOffsetOrUndefined
            val endOffset = ktTypeParameterDeclaration.endOffsetOrUndefined
            declareTypeParameter(
                startOffset,
                endOffset,
                typeParameterDescriptor
            )
        }

        for (irTypeParameter in irTypeParametersOwner.typeParameters) {
            irTypeParameter.descriptor.upperBounds.mapTo(irTypeParameter.superTypes) {
                it.toIrType()
            }
        }
    }

    fun generateInitializerBody(scopeOwnerSymbol: IrSymbol, ktBody: KtExpression): IrExpressionBody =
        createBodyGenerator(scopeOwnerSymbol).generateExpressionBody(ktBody)

    fun generateFakeOverrideDeclaration(memberDescriptor: CallableMemberDescriptor, ktElement: KtElement): IrDeclaration {
        assert(memberDescriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            "Fake override expected: $memberDescriptor"
        }
        return when (memberDescriptor) {
            is FunctionDescriptor ->
                generateFakeOverrideFunction(memberDescriptor, ktElement)
            is PropertyDescriptor ->
                generateFakeOverrideProperty(memberDescriptor, ktElement)
            else ->
                throw AssertionError("Unexpected member descriptor: $memberDescriptor")
        }
    }

    private fun generateFakeOverrideProperty(propertyDescriptor: PropertyDescriptor, ktElement: KtElement): IrProperty {
        val startOffset = ktElement.startOffset
        val endOffset = ktElement.endOffset

        val backingField =
            if (propertyDescriptor.getter == null)
                context.symbolTable.declareField(
                    startOffset, endOffset, IrDeclarationOrigin.FAKE_OVERRIDE,
                    propertyDescriptor, propertyDescriptor.type.toIrType()
                )
            else
                null

        return IrPropertyImpl(
            startOffset, endOffset,
            IrDeclarationOrigin.FAKE_OVERRIDE,
            false,
            propertyDescriptor,
            backingField,
            propertyDescriptor.getter?.let { generateFakeOverrideFunction(it, ktElement) },
            propertyDescriptor.setter?.let { generateFakeOverrideFunction(it, ktElement) }
        )
    }

    private fun generateFakeOverrideFunction(functionDescriptor: FunctionDescriptor, ktElement: KtElement): IrSimpleFunction =
        FunctionGenerator(this).generateFakeOverrideFunction(functionDescriptor, ktElement)
}

abstract class DeclarationGeneratorExtension(val declarationGenerator: DeclarationGenerator) : Generator {
    override val context: GeneratorContext get() = declarationGenerator.context

    inline fun <T : IrDeclaration> T.buildWithScope(builder: (T) -> Unit): T =
        also { irDeclaration ->
            context.symbolTable.withScope(irDeclaration.descriptor) {
                builder(irDeclaration)
            }
        }

    fun KotlinType.toIrType() = with(declarationGenerator) { toIrType() }
}

fun Generator.createBodyGenerator(scopeOwnerSymbol: IrSymbol) =
    BodyGenerator(scopeOwnerSymbol, context)