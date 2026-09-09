/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtIdeApi::class, KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs

import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

/** Base interface for all Kotlin stubs */
@KtImplementationDetail
interface KotlinStubElement<T : KtElement> : StubElement<T> {
    /**
     * Returns a copy of this stub with the parent set to [newParent].
     *
     * **Note**: the implementation doesn't guarantee that [com.intellij.psi.stubs.ObjectStubBase.isDangling] flag is copied
     */
    @KtImplementationDetail
    fun copyInto(newParent: StubElement<*>?): KotlinStubElement<T>

    /**
     * Returns whether two stubs have equivalent types and properties.
     * Doesn't compare children stubs or any other tree structure details.
     *
     * **Note**: This method shouldn't be used outside of compiler internals.
     * Stubs from different files aren't supposed to be comparable, that's why `equals` / `hashCode` are not implemented,
     * as they would lead to incorrect behavior.
     */
    @KtImplementationDetail
    fun isEquivalentTo(other: KotlinStubElement<*>): Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinFileStub : PsiFileStub<KtFile>, KotlinStubElement<KtFile> {
    @KtIdeApi
    fun getPackageFqName(): FqName = (kind as? KotlinFileStubKind.WithPackage)?.packageFqName ?: FqName.ROOT

    @KtImplementationDetail
    fun isScript(): Boolean = kind is KotlinFileStubKind.WithPackage.Script

    @KtImplementationDetail
    fun findImportsByAlias(alias: String): List<KotlinImportDirectiveStub>

    /** @see KotlinFileStubKind */
    @KtIdeApi
    val kind: KotlinFileStubKind
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinPlaceHolderStub<T : KtElement> : KotlinStubElement<T>

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinPlaceHolderWithTextStub<T : KtElement> : KotlinPlaceHolderStub<T> {
    @KtImplementationDetail
    val text: String
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinStubWithFqName<T : KtNamedDeclaration> : NamedStub<T>, KotlinStubElement<T> {
    @KtIdeApi
    val fqName: FqName?
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinClassifierStub<T : KtClassLikeDeclaration> : KotlinStubElement<T> {
    @KtImplementationDetail
    val classId: ClassId?
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinTypeAliasStub : KotlinClassifierStub<KtTypeAlias>, KotlinStubWithFqName<KtTypeAlias> {
    @KtImplementationDetail
    val isTopLevel: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinClassOrObjectStub<T : KtClassOrObject> : KotlinClassifierStub<T>, KotlinStubWithFqName<T> {
    @KtImplementationDetail
    val isLocal: Boolean get() = classId == null

    @KtIdeApi
    val superNames: List<String>

    @KtIdeApi
    val isTopLevel: Boolean

    /**
     * Raw KDoc text, if available.
     *
     * Currently, KDoc is only available for decompiled declaration.
     * For source ones one can read the KDoc content from the PSI directly.
     */
    @KtImplementationDetail
    val kdocText: String?
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinClassStub : KotlinClassOrObjectStub<KtClass> {
    @KtIdeApi
    val isInterface: Boolean

    /**
     * When we build [KotlinClassStub] for source stubs, this function always returns `false`. For binary stubs, it returns whether
     * the binary class was compiled with `-jvm-default={enable|no-compatibility}` option or not.
     */
    @KtImplementationDetail
    val isClsStubCompiledToJvmDefaultImplementation: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinObjectStub : KotlinClassOrObjectStub<KtObjectDeclaration> {
    @KtIdeApi
    val isObjectLiteral: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinValueArgumentStub<T : KtValueArgument> : KotlinPlaceHolderStub<T> {
    @KtImplementationDetail
    val isSpread: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinAnnotationEntryStub : KotlinStubElement<KtAnnotationEntry> {
    @KtIdeApi
    val shortName: String?

    @KtImplementationDetail
    val hasValueArguments: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinAnnotationUseSiteTargetStub : KotlinStubElement<KtAnnotationUseSiteTarget> {
    @KtImplementationDetail
    val useSiteTarget: String
}

/**
 * A marker interface for declarations with bodies.
 */
@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinDeclarationWithBodyStub<T : KtDeclarationWithBody> : KotlinStubElement<T> {
    /**
     * Whether the declaration may have a contract. **false** means that the declaration definitely has no contract, but **true** doesn't
     * guarantee that the declaration has a contract.
     */
    @KtImplementationDetail
    val mayHaveContract: Boolean

    /**
     * Whether the declaration has a block body or no bodies at all.
     */
    @KtImplementationDetail
    val hasNoExpressionBody: Boolean

    /**
     * Whether the declaration has a body (expression or block).
     */
    @KtImplementationDetail
    val hasBody: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinFunctionStub : KotlinCallableStubBase<KtNamedFunction>, KotlinDeclarationWithBodyStub<KtNamedFunction> {
    @KtImplementationDetail
    val hasTypeParameterListBeforeFunctionName: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinConstructorStub<T : KtConstructor<T>> : KotlinCallableStubBase<T>, KotlinDeclarationWithBodyStub<T> {
    @KtImplementationDetail
    val isDelegatedCallToThis: Boolean

    @KtImplementationDetail
    val isExplicitDelegationCall: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinImportAliasStub : NamedStub<KtImportAlias>, KotlinStubElement<KtImportAlias>

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinImportDirectiveStub : KotlinStubElement<KtImportDirective> {
    @KtImplementationDetail
    val isAllUnder: Boolean

    @KtImplementationDetail
    val importedFqName: FqName?

    @KtImplementationDetail
    val isValid: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinModifierListStub : KotlinStubElement<KtDeclarationModifierList> {
    @KtIdeApi
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
    @KtImplementationDetail
    val referencedName: String
}

@KtImplementationDetail
interface KotlinOperationReferenceExpressionStub : KotlinStubElement<KtOperationReferenceExpression> {
    /**
     * The name of the referenced operation.
     *
     * For operator symbols, this is the symbol itself (e.g., `-` for [org.jetbrains.kotlin.lexer.KtTokens.MINUS]).
     * For infix function calls, this is the function name (e.g., `shl`).
     *
     * @see org.jetbrains.kotlin.psi.KtOperationReferenceExpression.getReferencedName
     */
    val referencedName: String

    /**
     * The token type of the referenced operation.
     *
     * For infix function calls, this is [org.jetbrains.kotlin.lexer.KtTokens.IDENTIFIER].
     *
     * @see org.jetbrains.kotlin.psi.KtOperationReferenceExpression.getReferencedNameElementType
     */
    val operationToken: IElementType
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinEnumEntrySuperclassReferenceExpressionStub : KotlinStubElement<KtEnumEntrySuperclassReferenceExpression> {
    @KtImplementationDetail
    val referencedName: String
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinParameterStub : KotlinStubWithFqName<KtParameter> {
    @KtImplementationDetail
    val isMutable: Boolean

    @KtIdeApi
    val hasValOrVar: Boolean

    @KtImplementationDetail
    val hasDefaultValue: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinPropertyAccessorStub : KotlinDeclarationWithBodyStub<KtPropertyAccessor> {
    @KtImplementationDetail
    val isGetter: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinBackingFieldStub : KotlinStubElement<KtBackingField> {
    @KtImplementationDetail
    val hasInitializer: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinDestructuringDeclarationStub : KotlinStubElement<KtDestructuringDeclaration> {
    @KtImplementationDetail
    val isVar: Boolean

    @KtImplementationDetail
    val hasInitializer: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinPropertyStub : KotlinCallableStubBase<KtProperty> {
    @KtImplementationDetail
    val isVar: Boolean

    @KtImplementationDetail
    val hasDelegate: Boolean

    @KtImplementationDetail
    val hasDelegateExpression: Boolean

    @KtImplementationDetail
    val hasInitializer: Boolean

    @KtImplementationDetail
    val hasReturnTypeRef: Boolean

    /**
     * Whether the property has a backing field.
     * The property is supposed to work only for binary stubs.
     *
     * Returns **null** if the information is not available (e.g., for source stubs or unsupported platforms).
     */
    @KtImplementationDetail
    val hasBackingField: Boolean?
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinCallableStubBase<TDeclaration : KtCallableDeclaration> : KotlinStubWithFqName<TDeclaration> {
    @KtIdeApi
    val isTopLevel: Boolean

    @KtIdeApi
    val isExtension: Boolean

    /**
     * Raw KDoc text, if available.
     *
     * Currently, KDoc is only available for decompiled declaration.
     * For source ones one can read the KDoc content from the PSI directly.
     */
    @KtImplementationDetail
    val kdocText: String?
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinTypeParameterStub : KotlinStubWithFqName<KtTypeParameter>

@KtImplementationDetail
enum class ConstantValueKind {
    NULL,
    BOOLEAN_CONSTANT,
    FLOAT_CONSTANT,
    CHARACTER_CONSTANT,
    INTEGER_CONSTANT
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinConstantExpressionStub : KotlinStubElement<KtConstantExpression> {
    @KtImplementationDetail
    val kind: ConstantValueKind

    @KtImplementationDetail
    val value: String
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinCollectionLiteralExpressionStub : KotlinStubElement<KtCollectionLiteralExpression> {
    /**
     * The number of collection literals in the collection literal expression.
     *
     * For example, in the collection literal expression `[1, 2, 3]`, this function will return `3`.
     *
     * @see org.jetbrains.kotlin.psi.KtCollectionLiteralExpression.getInnerExpressions
     */
    @KtImplementationDetail
    val innerExpressionCount: Int
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinTypeProjectionStub : KotlinStubElement<KtTypeProjection> {
    @KtImplementationDetail
    val projectionKind: KtProjectionKind
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinUserTypeStub : KotlinStubElement<KtUserType>

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinFunctionTypeStub : KotlinStubElement<KtFunctionType>

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinScriptStub : KotlinStubWithFqName<KtScript> {
    @KtIdeApi
    override val fqName: FqName

    /**
     * Whether the script is a REPL snippet.
     *
     * @see KtScript.isReplSnippet
     */
    @KtImplementationDetail
    val isReplSnippet: Boolean
}

@SubclassOptInRequired(KtImplementationDetail::class)
interface KotlinContextReceiverStub : KotlinStubElement<KtContextReceiver> {
    @KtImplementationDetail
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
    @KtImplementationDetail
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
    @KtImplementationDetail
    val hasMultipleExpressions: Boolean
}
