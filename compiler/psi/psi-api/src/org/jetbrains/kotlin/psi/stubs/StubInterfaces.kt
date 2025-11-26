/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs

import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

/** Base interface for all Kotlin stubs */
@KtImplementationDetail
interface KotlinStubElement<T : KtElement> : StubElement<T> {
    /** Returns a copy of this stub with the parent set to [newParent] */
    @KtImplementationDetail
    fun copyInto(newParent: StubElement<*>?): KotlinStubElement<T>
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinFileStub : PsiFileStub<KtFile>, KotlinStubElement<KtFile> {
    fun getPackageFqName(): FqName = (kind as? KotlinFileStubKind.WithPackage)?.packageFqName ?: FqName.ROOT
    fun isScript(): Boolean = kind is KotlinFileStubKind.WithPackage.Script

    fun findImportsByAlias(alias: String): List<KotlinImportDirectiveStub>

    /** @see KotlinFileStubKind */
    val kind: KotlinFileStubKind
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinPlaceHolderStub<T : KtElement> : KotlinStubElement<T>

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinPlaceHolderWithTextStub<T : KtElement> : KotlinPlaceHolderStub<T> {
    val text: String
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinStubWithFqName<T : KtNamedDeclaration> : NamedStub<T>, KotlinStubElement<T> {
    val fqName: FqName?
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinClassifierStub<T : KtClassLikeDeclaration> : KotlinStubElement<T> {
    val classId: ClassId?
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinTypeAliasStub : KotlinClassifierStub<KtTypeAlias>, KotlinStubWithFqName<KtTypeAlias> {
    val isTopLevel: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinClassOrObjectStub<T : KtClassOrObject> : KotlinClassifierStub<T>, KotlinStubWithFqName<T> {
    val isLocal: Boolean get() = classId == null
    val superNames: List<String>
    val isTopLevel: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinClassStub : KotlinClassOrObjectStub<KtClass> {
    val isInterface: Boolean

    /**
     * When we build [KotlinClassStub] for source stubs, this function always returns `false`. For binary stubs, it returns whether
     * the binary class was compiled with `-jvm-default={enable|no-compatibility}` option or not.
     */
    val isClsStubCompiledToJvmDefaultImplementation: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinObjectStub : KotlinClassOrObjectStub<KtObjectDeclaration> {
    val isObjectLiteral: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinValueArgumentStub<T : KtValueArgument> : KotlinPlaceHolderStub<T> {
    val isSpread: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinContractEffectStub : KotlinPlaceHolderStub<KtContractEffect>

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinAnnotationEntryStub : KotlinStubElement<KtAnnotationEntry> {
    val shortName: String?
    val hasValueArguments: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinAnnotationUseSiteTargetStub : KotlinStubElement<KtAnnotationUseSiteTarget> {
    val useSiteTarget: String
}

/**
 * A marker interface for declarations with bodies.
 */
@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinDeclarationWithBodyStub<T : KtDeclarationWithBody> : KotlinStubElement<T> {
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

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinFunctionStub : KotlinCallableStubBase<KtNamedFunction>, KotlinDeclarationWithBodyStub<KtNamedFunction> {
    val hasTypeParameterListBeforeFunctionName: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinConstructorStub<T : KtConstructor<T>> : KotlinCallableStubBase<T>, KotlinDeclarationWithBodyStub<T> {
    val isDelegatedCallToThis: Boolean
    val isExplicitDelegationCall: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinImportAliasStub : NamedStub<KtImportAlias>, KotlinStubElement<KtImportAlias>

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinImportDirectiveStub : KotlinStubElement<KtImportDirective> {
    val isAllUnder: Boolean
    val isPackage: Boolean
    val importedFqName: FqName?
    val isValid: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinModifierListStub : KotlinStubElement<KtDeclarationModifierList> {
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

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinNameReferenceExpressionStub : KotlinStubElement<KtNameReferenceExpression> {
    val referencedName: String
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinEnumEntrySuperclassReferenceExpressionStub : KotlinStubElement<KtEnumEntrySuperclassReferenceExpression> {
    val referencedName: String
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinParameterStub : KotlinStubWithFqName<KtParameter> {
    val isMutable: Boolean
    val hasValOrVar: Boolean
    val hasDefaultValue: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinPropertyAccessorStub : KotlinDeclarationWithBodyStub<KtPropertyAccessor> {
    val isGetter: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinBackingFieldStub : KotlinStubElement<KtBackingField> {
    val hasInitializer: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinPropertyStub : KotlinCallableStubBase<KtProperty> {
    val isVar: Boolean
    val hasDelegate: Boolean
    val hasDelegateExpression: Boolean
    val hasInitializer: Boolean
    val hasReturnTypeRef: Boolean

    /**
     * Whether the property has a backing field.
     * The property is supposed to work only for binary stubs.
     *
     * Returns **null** if the information is not available (e.g., for source stubs or unsupported platforms).
     */
    val hasBackingField: Boolean?
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinCallableStubBase<TDeclaration : KtCallableDeclaration> : KotlinStubWithFqName<TDeclaration> {
    val isTopLevel: Boolean
    val isExtension: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinTypeParameterStub : KotlinStubWithFqName<KtTypeParameter>

enum class ConstantValueKind {
    NULL,
    BOOLEAN_CONSTANT,
    FLOAT_CONSTANT,
    CHARACTER_CONSTANT,
    INTEGER_CONSTANT
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinConstantExpressionStub : KotlinStubElement<KtConstantExpression> {
    val kind: ConstantValueKind
    val value: String
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinClassLiteralExpressionStub : KotlinStubElement<KtClassLiteralExpression>

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinCollectionLiteralExpressionStub : KotlinStubElement<KtCollectionLiteralExpression> {
    /**
     * The number of collection literals in the collection literal expression.
     *
     * For example, in the collection literal expression `[1, 2, 3]`, this function will return `3`.
     *
     * @see org.jetbrains.kotlin.psi.KtCollectionLiteralExpression.getInnerExpressions
     */
    val innerExpressionCount: Int
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinTypeProjectionStub : KotlinStubElement<KtTypeProjection> {
    val projectionKind: KtProjectionKind
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinUserTypeStub : KotlinStubElement<KtUserType>

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinFunctionTypeStub : KotlinStubElement<KtFunctionType>

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinScriptStub : KotlinStubWithFqName<KtScript> {
    override val fqName: FqName
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinContextReceiverStub : KotlinStubElement<KtContextReceiver> {
    val label: String?
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinStringInterpolationPrefixStub : KotlinStubElement<KtStringInterpolationPrefix> {
    /**
     * The count of `$` characters in the string interpolation prefix.
     *
     * For example, a single `$` in string interpolation would have count of 1,
     * while `$$` would have count of 2.
     */
    val dollarSignCount: Int
}

@SubclassOptInRequired(KtImplementationDetail::class)
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
