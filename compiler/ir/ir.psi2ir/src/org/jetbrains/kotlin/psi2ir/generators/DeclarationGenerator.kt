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

import org.jetbrains.kotlin.backend.common.BackendException
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.ScopeBuilder
import org.jetbrains.kotlin.ir.util.SignatureScope
import org.jetbrains.kotlin.ir.util.withLocalScope
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi.synthetics.findClassDescriptor
import org.jetbrains.kotlin.psi2ir.endOffsetOrUndefined
import org.jetbrains.kotlin.psi2ir.startOffsetOrUndefined
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.types.KotlinType

class DeclarationGenerator(override val context: GeneratorContext) : Generator {

    private val typeTranslator = context.typeTranslator

    fun KotlinType.toIrType() = typeTranslator.translateType(this)

    fun generateMemberDeclaration(ktDeclaration: KtDeclaration): IrDeclaration? {
        return try {
            when (ktDeclaration) {
                is KtNamedFunction ->
                    FunctionGenerator(this).generateFunctionDeclaration(ktDeclaration)
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

    fun generateClassMemberDeclaration(ktDeclaration: KtDeclaration, irClass: IrClass): IrDeclaration? =
        when (ktDeclaration) {
            is KtAnonymousInitializer ->
                AnonymousInitializerGenerator(this).generateAnonymousInitializerDeclaration(ktDeclaration, irClass)
            is KtSecondaryConstructor ->
                FunctionGenerator(this).generateSecondaryConstructor(ktDeclaration)
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
            context.symbolTable.declareTypeAlias(this) { symbol ->
                context.irFactory.createTypeAlias(
                    ktTypeAlias.startOffsetSkippingComments, ktTypeAlias.endOffset, symbol,
                    name, visibility, expandedType.toIrType(), isActual, IrDeclarationOrigin.DEFINED
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

class IrElementScopeBuilder : ScopeBuilder<DeclarationDescriptor, IrElement> {

    private class LocalIndexCollector(private val scope: SignatureScope<DeclarationDescriptor>) : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            if (declaration.kind == ClassKind.OBJECT) {
                // TODO: is that correct?
                scope.commitAnonymousObject(declaration.descriptor)
            } else {
                scope.commitLocalClass(declaration.descriptor)
            }
        }

        override fun visitFunction(declaration: IrFunction) {
            scope.commitLocalFunction(declaration.descriptor)
        }

        override fun visitFunctionExpression(expression: IrFunctionExpression) {
            scope.commitLambda(expression.function.descriptor)
        }

    }

    override fun build(scope: SignatureScope<DeclarationDescriptor>, element: IrElement?) {
        element?.acceptChildrenVoid(LocalIndexCollector(scope))
    }
}

class KtElementScopeBuilder(private val bindingContext: BindingContext) : ScopeBuilder<DeclarationDescriptor, KtElement> {

    private inner class LocalIndexCollector(private val scope: SignatureScope<DeclarationDescriptor>) : KtVisitorVoid() {

        private val KtPureClassOrObject.classDescriptor: ClassDescriptor
            get() = findClassDescriptor(bindingContext)

        private val KtFunction.functionDescriptor: FunctionDescriptor
            get() = bindingContext[BindingContext.FUNCTION, this] ?: error("No descriptor found for function $text")

        override fun visitKtElement(element: KtElement) {
            element.acceptChildren(this)
        }

        override fun visitLambdaExpression(expression: KtLambdaExpression) {
            scope.commitLambda(expression.functionLiteral.functionDescriptor)
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            scope.commitLocalFunction(function.functionDescriptor)
        }

        override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
            scope.commitAnonymousObject(expression.objectDeclaration.classDescriptor)
        }

        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
            scope.commitLocalClass(classOrObject.classDescriptor)
        }

        override fun visitEnumEntry(enumEntry: KtEnumEntry) {
            enumEntry.initializerList?.initializers?.forEach { it.accept(this) }
//            enumEntry.acceptChildren(this)
        }
    }

    override fun build(scope: SignatureScope<DeclarationDescriptor>, element: KtElement?) {
        element?.acceptChildren(LocalIndexCollector(scope))
    }
}

object EmptyScopeBuilder : ScopeBuilder<DeclarationDescriptor, KtElement> {
    override fun build(scope: SignatureScope<DeclarationDescriptor>, element: KtElement?) {}
}

abstract class DeclarationGeneratorExtension(val declarationGenerator: DeclarationGenerator) : Generator {
    override val context: GeneratorContext get() = declarationGenerator.context

    inline fun <T : IrDeclaration> T.buildWithScope(builder: (T) -> Unit): T =
        also { irDeclaration ->
            context.symbolTable.withScope(irDeclaration) {
                builder(irDeclaration)
            }
        }

    inline fun <E : KtElement, T : IrDeclaration> T.buildWithLocalScope(element: E?, crossinline builder: (T) -> Unit): T =
        also { irDeclaration ->
            context.symbolTable.withLocalScope(element, KtElementScopeBuilder(context.bindingContext), irDeclaration) {
                builder(irDeclaration)
            }
        }


    fun KotlinType.toIrType() = with(declarationGenerator) { toIrType() }
}

fun Generator.createBodyGenerator(scopeOwnerSymbol: IrSymbol) =
    BodyGenerator(scopeOwnerSymbol, context)
