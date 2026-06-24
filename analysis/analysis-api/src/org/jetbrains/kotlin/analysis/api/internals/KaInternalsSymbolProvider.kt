/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsSymbolProvider {
    public fun symbol(declaration: KtDeclaration): KaDeclarationSymbol

    public fun symbol(parameter: KtParameter): KaVariableSymbol

    public fun symbol(namedFunction: KtNamedFunction): KaFunctionSymbol

    public fun symbol(constructor: KtConstructor<*>): KaConstructorSymbol

    public fun symbol(typeParameter: KtTypeParameter): KaTypeParameterSymbol

    public fun symbol(typeAlias: KtTypeAlias): KaTypeAliasSymbol

    public fun symbol(enumEntry: KtEnumEntry): KaEnumEntrySymbol

    public fun symbol(functionLiteral: KtFunctionLiteral): KaAnonymousFunctionSymbol

    public fun symbol(property: KtProperty): KaVariableSymbol

    public fun symbol(backingField: KtBackingField): KaBackingFieldSymbol

    public fun symbol(objectLiteralExpression: KtObjectLiteralExpression): KaAnonymousObjectSymbol

    public fun symbol(objectDeclaration: KtObjectDeclaration): KaClassSymbol

    public fun classSymbol(classOrObject: KtClassOrObject): KaClassSymbol?

    public fun namedClassSymbol(classOrObject: KtClassOrObject): KaNamedClassSymbol?

    public fun symbol(propertyAccessor: KtPropertyAccessor): KaPropertyAccessorSymbol

    public fun symbol(classInitializer: KtClassInitializer): KaClassInitializerSymbol

    public fun symbol(entry: KtDestructuringDeclarationEntry): KaVariableSymbol

    public fun symbol(destructuringDeclaration: KtDestructuringDeclaration): KaDestructuringDeclarationSymbol

    public fun symbol(file: KtFile): KaFileSymbol

    public fun symbol(script: KtScript): KaScriptSymbol

    @KaExperimentalApi
    public fun symbol(contextReceiver: KtContextReceiver): KaContextParameterSymbol

    public fun findPackage(fqName: FqName): KaPackageSymbol?

    public fun findClass(classId: ClassId): KaNamedClassSymbol?

    public fun findTypeAlias(classId: ClassId): KaTypeAliasSymbol?

    public fun findClassLike(classId: ClassId): KaClassLikeSymbol?

    public fun findTopLevelCallables(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol>

    public val rootPackageSymbol: KaPackageSymbol
}
