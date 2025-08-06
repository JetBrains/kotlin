/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs

import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

interface KotlinFileStub : PsiFileStub<KtFile> {
    fun getPackageFqName(): FqName = (kind as? KotlinFileStubKind.WithPackage)?.packageFqName ?: FqName.ROOT
    fun isScript(): Boolean = kind is KotlinFileStubKind.WithPackage.Script

    fun findImportsByAlias(alias: String): List<KotlinImportDirectiveStub>

    /** @see KotlinFileStubKind */
    val kind: KotlinFileStubKind
}

interface KotlinPlaceHolderStub<T : KtElement> : StubElement<T>

interface KotlinPlaceHolderWithTextStub<T : KtElement> : KotlinPlaceHolderStub<T> {
    val text: String
}

interface KotlinStubWithFqName<T : PsiNamedElement> : NamedStub<T> {
    val fqName: FqName?
}

interface KotlinClassifierStub {
    val classId: ClassId?
}

interface KotlinTypeAliasStub : KotlinClassifierStub, KotlinStubWithFqName<KtTypeAlias> {
    val isTopLevel: Boolean
}

interface KotlinClassOrObjectStub<T : KtClassOrObject> : KotlinClassifierStub, KotlinStubWithFqName<T> {
    val isLocal: Boolean
    val superNames: List<String>
    val isTopLevel: Boolean
}

interface KotlinClassStub : KotlinClassOrObjectStub<KtClass> {
    val isInterface: Boolean
    val isEnumEntry: Boolean

    /**
     * When we build [KotlinClassStub] for source stubs, this function always returns `false`. For binary stubs, it returns whether
     * the binary class was compiled with `-jvm-default={enable|no-compatibility}` option or not.
     */
    val isClsStubCompiledToJvmDefaultImplementation: Boolean
}

interface KotlinObjectStub : KotlinClassOrObjectStub<KtObjectDeclaration> {
    val isObjectLiteral: Boolean
}

interface KotlinValueArgumentStub<T : KtValueArgument> : KotlinPlaceHolderStub<T> {
    val isSpread: Boolean
}

interface KotlinContractEffectStub : KotlinPlaceHolderStub<KtContractEffect> {}

interface KotlinAnnotationEntryStub : StubElement<KtAnnotationEntry> {
    val shortName: String?
    val hasValueArguments: Boolean
}

interface KotlinAnnotationUseSiteTargetStub : StubElement<KtAnnotationUseSiteTarget> {
    val useSiteTarget: String
}

/**
 * A marker interface for declarations with bodies.
 */
interface KotlinDeclarationWithBodyStub<T : KtDeclarationWithBody> : StubElement<T> {
    /**
     * Whether the declaration may have a contract.
     * **false** means that the declaration is definitely having no contract,
     * but **true** doesn't guarantee that the declaration has a contract.
     */
    val mayHaveContract: Boolean

    /**
     * Whether the declaration has a block body or no bodies at all.
     */
    val hasNoExpressionBody: Boolean

    /**
     * Whether the declaration has a body (expression or block).
     */
    val hasBody: Boolean
}

interface KotlinFunctionStub : KotlinCallableStubBase<KtNamedFunction>, KotlinDeclarationWithBodyStub<KtNamedFunction> {
    val hasTypeParameterListBeforeFunctionName: Boolean
}

interface KotlinConstructorStub<T : KtConstructor<T>> : KotlinCallableStubBase<T>, KotlinDeclarationWithBodyStub<T> {
    val isDelegatedCallToThis: Boolean
    val isExplicitDelegationCall: Boolean
}

interface KotlinImportAliasStub : NamedStub<KtImportAlias>

interface KotlinImportDirectiveStub : StubElement<KtImportDirective> {
    val isAllUnder: Boolean
    val importedFqName: FqName?
    val isValid: Boolean
}

interface KotlinModifierListStub : StubElement<KtDeclarationModifierList> {
    fun hasModifier(modifierToken: KtModifierKeywordToken): Boolean

    /**
     * Whether the modifier list has a [SpecialFlag].
     */
    @KtImplementationDetail
    fun hasSpecialFlag(flag: SpecialFlag): Boolean

    /** Represents special flags that are common for many declarations */
    @KtImplementationDetail
    enum class SpecialFlag {
        /**
         * Whether the return value of the modifier list owner must be checked for usage.
         * This check is supposed to work only for binary stubs.
         *
         * See org.jetbrains.kotlin.resolve.ReturnValueStatus and FirResolvedStatus for details.
         * Feature issue: [KT-12719](https://youtrack.jetbrains.com/issue/KT-12719).
         */
        MustUseReturnValue,

        /**
         * Whether the return value of the modifier list owner is declared as explicitly ignorable and should not be checked for usage.
         * This check is supposed to work only for binary stubs.
         *
         * See org.jetbrains.kotlin.resolve.ReturnValueStatus and FirResolvedStatus for details.
         * Feature issue: [KT-12719](https://youtrack.jetbrains.com/issue/KT-12719).
         */
        IgnorableReturnValue,
    }
}

interface KotlinNameReferenceExpressionStub : StubElement<KtNameReferenceExpression> {
    val referencedName: String
}

interface KotlinEnumEntrySuperclassReferenceExpressionStub : StubElement<KtEnumEntrySuperclassReferenceExpression> {
    fun getReferencedName(): String
}

interface KotlinParameterStub : KotlinStubWithFqName<KtParameter> {
    fun isMutable(): Boolean
    fun hasValOrVar(): Boolean
    fun hasDefaultValue(): Boolean
}

interface KotlinPropertyAccessorStub : KotlinDeclarationWithBodyStub<KtPropertyAccessor> {
    fun isGetter(): Boolean
}

interface KotlinBackingFieldStub : StubElement<KtBackingField> {
    fun hasInitializer(): Boolean
}

interface KotlinPropertyStub : KotlinCallableStubBase<KtProperty> {
    fun isVar(): Boolean
    fun hasDelegate(): Boolean
    fun hasDelegateExpression(): Boolean
    fun hasInitializer(): Boolean
    fun hasReturnTypeRef(): Boolean

    /**
     * Whether the property has a backing field.
     * The property is supposed to work only for binary stubs.
     *
     * Returns **null** if the information is not available (e.g., for source stubs or unsupported platforms).
     */
    val hasBackingField: Boolean?
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

interface KotlinClassLiteralExpressionStub : StubElement<KtClassLiteralExpression>
interface KotlinCollectionLiteralExpressionStub : StubElement<KtCollectionLiteralExpression> {
    /**
     * The number of collection literals in the collection literal expression.
     *
     * For example, in the collection literal expression `[1, 2, 3]`, this function will return `3`.
     *
     * @see org.jetbrains.kotlin.psi.KtCollectionLiteralExpression.getInnerExpressions
     */
    val innerExpressionCount: Int
}

interface KotlinTypeProjectionStub : StubElement<KtTypeProjection> {
    fun getProjectionKind(): KtProjectionKind
}

interface KotlinUserTypeStub : StubElement<KtUserType>

interface KotlinFunctionTypeStub : StubElement<KtFunctionType>

interface KotlinScriptStub : KotlinStubWithFqName<KtScript> {
    override val fqName: FqName
}

interface KotlinContextReceiverStub : StubElement<KtContextReceiver> {
    fun getLabel(): String?
}

interface KotlinStringInterpolationPrefixStub : StubElement<KtStringInterpolationPrefix> {
    /**
     * The count of `$` characters in the string interpolation prefix.
     *
     * For example, a single `$` in string interpolation would have count of 1,
     * while `$$` would have count of 2.
     */
    val dollarSignCount: Int
}

interface KotlinBlockStringTemplateEntryStub : KotlinPlaceHolderWithTextStub<KtBlockStringTemplateEntry> {
    /**
     * Whether the entry has more than one expression which is illegal code.
     *
     * ### Examples
     *
     * ```kotlin
     * @InvalidAnnotation("${CONSTANT ${}}")
     * fun foo() {}
     * ```
     */
    val hasMultipleExpressions: Boolean
}
