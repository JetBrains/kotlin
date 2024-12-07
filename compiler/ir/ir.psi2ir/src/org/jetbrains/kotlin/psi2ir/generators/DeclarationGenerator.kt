/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.backend.common.BackendException
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.endOffsetOrUndefined
import org.jetbrains.kotlin.psi2ir.startOffsetOrUndefined
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded

class DeclarationGenerator(override val context: GeneratorContext) : Generator {

    private val typeTranslator = context.typeTranslator

    fun KotlinType.toIrType() = typeTranslator.translateType(this)

    fun generateMemberDeclaration(ktDeclaration: KtDeclaration): IrDeclaration? {
        return try {
            when (ktDeclaration) {
                is KtNamedFunction ->
                    FunctionGenerator(this).generateFunctionDeclaration(ktDeclaration, null)
                is KtProperty ->
                    PropertyGenerator(this).generatePropertyDeclaration(ktDeclaration)
                is KtClassOrObject ->
                    generateClassOrObjectDeclaration(ktDeclaration)
                is KtTypeAlias ->
                    generateTypeAliasDeclaration(ktDeclaration)
                is KtScript ->
                    ScriptGenerator(this).generateScriptDeclaration(ktDeclaration)
                else ->
                    context.irFactory.createErrorDeclaration(
                        ktDeclaration.startOffsetSkippingComments, ktDeclaration.endOffset,
                        getOrFail(BindingContext.DECLARATION_TO_DESCRIPTOR, ktDeclaration)
                    )
            }
        } catch (e: BackendException) {
            throw e
        } catch (e: Throwable) {
            rethrowIntellijPlatformExceptionIfNeeded(e)
            when {
                context.configuration.ignoreErrors -> {
                    context.irFactory.createErrorDeclaration(
                        ktDeclaration.startOffsetSkippingComments, ktDeclaration.endOffset,
                        getOrFail(BindingContext.DECLARATION_TO_DESCRIPTOR, ktDeclaration)
                    )
                }
                e is ErrorExpressionException ->
                    CodegenUtil.reportBackendException(e.cause ?: e, "psi2ir", PsiDiagnosticUtils.atLocation(e.ktElement), e.message)
                else -> {
                    val psiFile = ktDeclaration.containingKtFile
                    CodegenUtil.reportBackendException(e, "psi2ir", psiFile.virtualFile?.path ?: psiFile.name)
                }
            }
        }
    }

    fun generateSyntheticClassOrObject(syntheticDeclaration: KtPureClassOrObject): IrClass {
        return generateClassOrObjectDeclaration(syntheticDeclaration)
    }

    fun generateClassMemberDeclaration(
        ktDeclaration: KtDeclaration,
        irClass: IrClass,
        ktClassOrObject: KtPureClassOrObject
    ): IrDeclaration? =
        when (ktDeclaration) {
            is KtAnonymousInitializer ->
                AnonymousInitializerGenerator(this).generateAnonymousInitializerDeclaration(ktDeclaration, irClass)
            is KtSecondaryConstructor ->
                FunctionGenerator(this).generateSecondaryConstructor(ktDeclaration, ktClassOrObject)
            is KtEnumEntry ->
                generateEnumEntryDeclaration(ktDeclaration)
            else ->
                generateMemberDeclaration(ktDeclaration)
        }

    private fun generateEnumEntryDeclaration(ktEnumEntry: KtEnumEntry): IrEnumEntry =
        ClassGenerator(this).generateEnumEntry(ktEnumEntry)

    fun generateClassOrObjectDeclaration(ktClassOrObject: KtPureClassOrObject): IrClass =
        ClassGenerator(this).generateClass(ktClassOrObject)

    private fun generateTypeAliasDeclaration(ktTypeAlias: KtTypeAlias): IrTypeAlias =
        with(getOrFail(BindingContext.TYPE_ALIAS, ktTypeAlias)) {
            context.symbolTable.descriptorExtension.declareTypeAlias(this) { symbol: IrTypeAliasSymbol ->
                context.irFactory.createTypeAlias(
                    startOffset = ktTypeAlias.startOffsetSkippingComments,
                    endOffset = ktTypeAlias.endOffset,
                    origin = IrDeclarationOrigin.DEFINED,
                    name = name,
                    visibility = visibility,
                    symbol = symbol,
                    isActual = isActual,
                    expandedType = expandedType.toIrType()
                )
            }.also {
                generateGlobalTypeParametersDeclarations(it, declaredTypeParameters)
            }
        }

    fun generateGlobalTypeParametersDeclarations(
        irTypeParametersOwner: IrTypeParametersContainer,
        from: List<TypeParameterDescriptor>
    ) {
        generateTypeParameterDeclarations(irTypeParametersOwner, from) { startOffset, endOffset, typeParameterDescriptor ->
            context.symbolTable.descriptorExtension.declareGlobalTypeParameter(
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
            context.symbolTable.descriptorExtension.declareScopedTypeParameter(
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
        irTypeParametersOwner.typeParameters += from.map { typeParameterDescriptor ->
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
            irTypeParameter.superTypes = irTypeParameter.descriptor.upperBounds.map {
                it.toIrType()
            }
        }
    }

    fun generateInitializerBody(scopeOwnerSymbol: IrSymbol, ktBody: KtExpression): IrExpressionBody =
        createBodyGenerator(scopeOwnerSymbol).generateExpressionBody(ktBody)

    fun generateFakeOverrideDeclaration(memberDescriptor: CallableMemberDescriptor, ktElement: KtPureElement): IrDeclaration? {
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

    private fun generateFakeOverrideProperty(propertyDescriptor: PropertyDescriptor, ktElement: KtPureElement): IrProperty? =
        PropertyGenerator(this).generateFakeOverrideProperty(propertyDescriptor, ktElement)

    private fun generateFakeOverrideFunction(functionDescriptor: FunctionDescriptor, ktElement: KtPureElement): IrSimpleFunction? =
        FunctionGenerator(this).generateFakeOverrideFunction(functionDescriptor, ktElement)
}

abstract class DeclarationGeneratorExtension(val declarationGenerator: DeclarationGenerator) : Generator {
    override val context: GeneratorContext get() = declarationGenerator.context

    inline fun <T : IrDeclaration> T.buildWithScope(builder: (T) -> Unit): T =
        also { irDeclaration ->
            context.symbolTable.withScope(irDeclaration) {
                builder(irDeclaration)
            }
        }

    fun KotlinType.toIrType() = with(declarationGenerator) { toIrType() }
}

internal fun Generator.createBodyGenerator(scopeOwnerSymbol: IrSymbol, parentLoopResolver: LoopResolver? = null) =
    BodyGenerator(scopeOwnerSymbol, context, parentLoopResolver)
