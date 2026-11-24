/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.components.KaSessionComponentImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

/**
 * [KaSymbolProvider] provides [KaSymbol]s for given PSI elements.
 *
 * **Important**: Symbols can be created only for elements which are a part of the current [KaSession][org.jetbrains.kotlin.analysis.api.KaSession].
 *
 * @see org.jetbrains.kotlin.analysis.api.KaSession
 */
@Deprecated(
    message = "Session components are an implementation detail of the legacy 'KaSession' API. Use the context-parameter API instead.",
    level = DeprecationLevel.WARNING,
)
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaSymbolProvider : KaLifetimeOwner {
    /**
     * A [KaDeclarationSymbol] for the given [KtDeclaration].
     *
     * There are more specific `symbol` endpoints, such as [KtNamedFunction.symbol] and [KtClassOrObject.classSymbol], which can be used
     * when more specific PSI elements are available.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtDeclaration.symbol: KaDeclarationSymbol

    /**
     * A [KaVariableSymbol] for the given [KtParameter].
     *
     * Unfortunately, [KtParameter] in PSI stands for many things, and not all of them are represented by a single type of symbol,
     * so this function does not work for all possible [KtParameter]s.
     *
     * If [KtParameter.isFunctionTypeParameter] is `true`, i.e. if the given [KtParameter] is used as a function type parameter,
     * it is not possible to create [KaValueParameterSymbol], hence an error will be raised.
     *
     * If [KtParameter.isLoopParameter] is `true`, i.e. if the given [KtParameter] is a loop variable in `for` expression, then the symbol is
     * [KaLocalVariableSymbol].
     *
     * If [KtParameter.isContextParameter] is `true`, i.e. if the given [KtParameter] is used as a context parameter, then the symbol is
     * [KaContextParameterSymbol].
     *
     * Otherwise, the symbol is [KaValueParameterSymbol].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtParameter.symbol: KaVariableSymbol

    /**
     * A [KaFunctionSymbol] for the given [KtNamedFunction].
     *
     * If [KtNamedFunction.getName] is `null`, the symbol is a [KaAnonymousFunctionSymbol], and otherwise a [KaNamedFunctionSymbol].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtNamedFunction.symbol: KaFunctionSymbol

    /**
     * A [KaConstructorSymbol] for the given [KtConstructor].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtConstructor<*>.symbol: KaConstructorSymbol

    /**
     * A [KaTypeParameterSymbol] for the given [KtTypeParameter].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtTypeParameter.symbol: KaTypeParameterSymbol

    /**
     * A [KaTypeAliasSymbol] for the given [KtTypeAlias].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtTypeAlias.symbol: KaTypeAliasSymbol

    /**
     * A [KaEnumEntrySymbol] for the given [KtEnumEntry].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtEnumEntry.symbol: KaEnumEntrySymbol

    /**
     * A [KaAnonymousFunctionSymbol] for the given [KtFunctionLiteral].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtFunctionLiteral.symbol: KaAnonymousFunctionSymbol

    /**
     * A [KaVariableSymbol] for the given [KtProperty].
     *
     * The symbol is a [KaKotlinPropertySymbol] for non-local properties, and a [KaLocalVariableSymbol] for local ones.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtProperty.symbol: KaVariableSymbol

    /**
     * A [KaBackingFieldSymbol] for the given [KtBackingField].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtBackingField.symbol: KaBackingFieldSymbol

    /**
     * A [KaAnonymousObjectSymbol] for the given [KtObjectLiteralExpression].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtObjectLiteralExpression.symbol: KaAnonymousObjectSymbol

    /**
     * A [KaClassSymbol] for the given [KtClassOrObject], or `null` for [KtEnumEntry] declarations.
     *
     * To retrieve a [KaEnumEntrySymbol], please refer to [KtEnumEntry.symbol].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.classSymbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.classSymbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.classSymbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtClassOrObject.classSymbol: KaClassSymbol?

    /**
     * A [KaClassSymbol] for the given [KtObjectDeclaration].
     *
     * The symbol may either be a [KaAnonymousObjectSymbol] if the given declaration is an [object expression](https://kotlinlang.org/docs/object-declarations.html#object-expressions),
     * or a [KaNamedClassSymbol] if it is a named object declaration.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtObjectDeclaration.symbol: KaClassSymbol

    /**
     * A [KaNamedClassSymbol] for the given named [KtClassOrObject], or `null` for [KtEnumEntry] declarations and object literals.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.namedClassSymbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.namedClassSymbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.namedClassSymbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtClassOrObject.namedClassSymbol: KaNamedClassSymbol?

    /**
     * A [KaPropertyAccessorSymbol] for the given [KtPropertyAccessor].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtPropertyAccessor.symbol: KaPropertyAccessorSymbol

    /**
     * A [KaClassInitializerSymbol] for the given [KtClassInitializer].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtClassInitializer.symbol: KaClassInitializerSymbol

    /**
     * A [KaVariableSymbol] that corresponds to the local variable introduced by the given [KtDestructuringDeclarationEntry].
     *
     * The symbol is usually a [KaLocalVariableSymbol]. However, for a top-level destructuring declaration in a script, the symbol is a
     * [KaKotlinPropertySymbol].
     *
     * #### Example
     *
     * ```kotlin
     * val (x, y) = p
     * ```
     *
     * The destructuring declaration above has two entries, one corresponding to `x` and another to `y`. For both of these entries, we can
     * retrieve a [KaVariableSymbol] which describes the entry.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtDestructuringDeclarationEntry.symbol: KaVariableSymbol

    /**
     * A [KaDestructuringDeclarationSymbol] for the given [KtDestructuringDeclaration].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtDestructuringDeclaration.symbol: KaDestructuringDeclarationSymbol

    /**
     * A [KaFileSymbol] for a [KtFile].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtFile.symbol: KaFileSymbol

    /**
     * A [KaScriptSymbol] for a [KtScript].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val KtScript.symbol: KaScriptSymbol

    /**
     * Represents [KtContextReceiver] as a [KaContextParameterSymbol].
     *
     * This is a temporary API for simplicity during the transition from context receivers to context parameters.
     *
     * **Note**: context receivers inside [KtFunctionType] are not supported.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.symbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.symbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.symbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public val KtContextReceiver.symbol: KaContextParameterSymbol

    /**
     * Returns a [KaPackageSymbol] corresponding to the given [fqName] if that package exists and is visible from the current use site, or
     * `null` otherwise.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.findPackage' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "findPackage(fqName)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.findPackage",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public fun findPackage(fqName: FqName): KaPackageSymbol?

    /**
     * Returns a [KaClassSymbol] for the specified [ClassId], or `null` if such a symbol cannot be found.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.findClass' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "findClass(classId)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.findClass",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public fun findClass(classId: ClassId): KaClassSymbol?

    /**
     * Returns a [KaTypeAliasSymbol] for the specified [ClassId], or `null` if such a symbol cannot be found.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.findTypeAlias' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "findTypeAlias(classId)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.findTypeAlias",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public fun findTypeAlias(classId: ClassId): KaTypeAliasSymbol?

    /**
     * Returns a [KaClassLikeSymbol] for the specified [ClassId], or `null` if such a symbol cannot be found.
     *
     * The function combines both class search (see [findClass]) and type alias search (see [findTypeAlias]).
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.findClassLike' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "findClassLike(classId)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.findClassLike",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public fun findClassLike(classId: ClassId): KaClassLikeSymbol?

    /**
     * Finds top-level functions and properties called [name] in the package called [packageFqName]. Returns only symbols that are visible
     * from the current use-site module.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.findTopLevelCallables' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "findTopLevelCallables(packageFqName, name)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.findTopLevelCallables",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public fun findTopLevelCallables(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol>

    /**
     * A [KaPackageSymbol] for the *root package*, which is the special package with an empty fully-qualified name.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.rootPackageSymbol' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "rootPackageSymbol",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.rootPackageSymbol",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public val rootPackageSymbol: KaPackageSymbol
}
