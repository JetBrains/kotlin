/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi.stubs

import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

interface KotlinFileStub : PsiFileStub<KtFile> {
    fun getPackageFqName(): FqName
    fun isScript(): Boolean
    fun findImportsByAlias(alias: String): List<KotlinImportDirectiveStub>
}

interface KotlinPlaceHolderStub<T : KtElement> : StubElement<T>

interface KotlinPlaceHolderWithTextStub<T : KtElement> : KotlinPlaceHolderStub<T> {
    fun text(): String
}

interface KotlinStubWithFqName<T : PsiNamedElement> : NamedStub<T> {
    fun getFqName(): FqName?
}

interface KotlinTypeAliasStub : KotlinStubWithFqName<KtTypeAlias> {
    fun isTopLevel(): Boolean
}

interface KotlinClassOrObjectStub<T : KtClassOrObject> : KotlinStubWithFqName<T> {
    fun isLocal(): Boolean
    fun getSuperNames(): List<String>
    fun isTopLevel(): Boolean
}

interface KotlinClassStub : KotlinClassOrObjectStub<KtClass> {
    fun isInterface(): Boolean
    fun isEnumEntry(): Boolean
}

interface KotlinObjectStub : KotlinClassOrObjectStub<KtObjectDeclaration> {
    fun isCompanion(): Boolean
    fun isObjectLiteral(): Boolean
}

interface KotlinValueArgumentStub<T : KtValueArgument> : KotlinPlaceHolderStub<T> {
    fun isSpread(): Boolean
}

interface KotlinContractEffectStub : KotlinPlaceHolderStub<KtContractEffect> {}

interface KotlinAnnotationEntryStub : StubElement<KtAnnotationEntry> {
    fun getShortName(): String?
    fun hasValueArguments(): Boolean
}

interface KotlinAnnotationUseSiteTargetStub : StubElement<KtAnnotationUseSiteTarget> {
    fun getUseSiteTarget(): String
}

interface KotlinFunctionStub : KotlinCallableStubBase<KtNamedFunction> {
    fun hasBlockBody(): Boolean
    fun hasBody(): Boolean
    fun hasTypeParameterListBeforeFunctionName(): Boolean
    fun mayHaveContract(): Boolean
}

interface KotlinImportAliasStub : StubElement<KtImportAlias> {
    fun getName(): String?
}

interface KotlinImportDirectiveStub : StubElement<KtImportDirective> {
    fun isAllUnder(): Boolean
    fun getImportedFqName(): FqName?
    fun isValid(): Boolean
}

interface KotlinModifierListStub : StubElement<KtDeclarationModifierList> {
    fun hasModifier(modifierToken: KtModifierKeywordToken): Boolean
}

interface KotlinNameReferenceExpressionStub : StubElement<KtNameReferenceExpression> {
    fun getReferencedName(): String
}

interface KotlinEnumEntrySuperclassReferenceExpressionStub : StubElement<KtEnumEntrySuperclassReferenceExpression> {
    fun getReferencedName(): String
}

interface KotlinParameterStub : KotlinStubWithFqName<KtParameter> {
    fun isMutable(): Boolean
    fun hasValOrVar(): Boolean
    fun hasDefaultValue(): Boolean
}

interface KotlinPropertyAccessorStub : StubElement<KtPropertyAccessor> {
    fun isGetter(): Boolean
    fun hasBody(): Boolean
    fun hasBlockBody(): Boolean
}

interface KotlinPropertyStub : KotlinCallableStubBase<KtProperty> {
    fun isVar(): Boolean
    fun hasDelegate(): Boolean
    fun hasDelegateExpression(): Boolean
    fun hasInitializer(): Boolean
    fun hasReturnTypeRef(): Boolean
}

interface KotlinCallableStubBase<TDeclaration : KtCallableDeclaration> : KotlinStubWithFqName<TDeclaration> {
    fun isTopLevel(): Boolean
    fun isExtension(): Boolean
}

interface KotlinTypeParameterStub : KotlinStubWithFqName<KtTypeParameter> {
    fun isInVariance(): Boolean
    fun isOutVariance(): Boolean
}

enum class ConstantValueKind {
    NULL,
    BOOLEAN_CONSTANT,
    FLOAT_CONSTANT,
    CHARACTER_CONSTANT,
    INTEGER_CONSTANT
}

interface KotlinConstantExpressionStub : StubElement<KtConstantExpression> {
    fun kind(): ConstantValueKind
    fun value(): String
}

interface KotlinTypeProjectionStub : StubElement<KtTypeProjection> {
    fun getProjectionKind(): KtProjectionKind
}

interface KotlinUserTypeStub : StubElement<KtUserType>

interface KotlinScriptStub : KotlinStubWithFqName<KtScript> {
    override fun getFqName(): FqName
}
