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
import org.jetbrains.kotlin.name.FqName
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.PsiFileStub
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.*

public interface KotlinFileStub : PsiFileStub<KtFile> {
    public fun getPackageFqName(): FqName
    public fun isScript(): Boolean
    public fun findImportsByAlias(alias: String): List<KotlinImportDirectiveStub>
}

public interface KotlinPlaceHolderStub<T : KtElement> : StubElement<T>

public interface KotlinStubWithFqName<T : PsiNamedElement> : NamedStub<T> {
    public fun getFqName(): FqName?
}

public interface KotlinClassOrObjectStub<T : KtClassOrObject> : KotlinStubWithFqName<T> {
    public fun isLocal(): Boolean
    public fun getSuperNames(): List<String>
    public fun isTopLevel(): Boolean
}

public interface KotlinClassStub : KotlinClassOrObjectStub<KtClass> {
    public fun isInterface(): Boolean
    public fun isEnumEntry(): Boolean
}

public interface KotlinObjectStub : KotlinClassOrObjectStub<KtObjectDeclaration> {
    public fun isCompanion(): Boolean
    public fun isObjectLiteral(): Boolean
}

public interface KotlinAnnotationEntryStub : StubElement<KtAnnotationEntry> {
    public fun getShortName(): String
    public fun hasValueArguments(): Boolean
}

public interface KotlinAnnotationUseSiteTargetStub : StubElement<KtAnnotationUseSiteTarget> {
    public fun getUseSiteTarget(): String
}

public interface KotlinFunctionStub : KotlinCallableStubBase<KtNamedFunction> {
    public fun hasBlockBody(): Boolean
    public fun hasBody(): Boolean
    public fun hasTypeParameterListBeforeFunctionName(): Boolean
}

public interface KotlinImportDirectiveStub : StubElement<KtImportDirective> {
    public fun isAbsoluteInRootPackage(): Boolean
    public fun isAllUnder(): Boolean
    public fun getImportedFqName(): FqName?
    public fun getAliasName(): String?
    public fun isValid(): Boolean
}

public interface KotlinModifierListStub : StubElement<KtModifierList> {
    public fun hasModifier(modifierToken: KtModifierKeywordToken): Boolean
}

public interface KotlinNameReferenceExpressionStub : StubElement<KtNameReferenceExpression> {
    public fun getReferencedName(): String
}

public interface KotlinEnumEntrySuperclassReferenceExpressionStub : StubElement<KtEnumEntrySuperclassReferenceExpression> {
    public fun getReferencedName(): String
}

public interface KotlinParameterStub : KotlinStubWithFqName<KtParameter> {
    public fun isMutable(): Boolean
    public fun hasValOrVar(): Boolean
    public fun hasDefaultValue(): Boolean
}

public interface KotlinPropertyAccessorStub : StubElement<KtPropertyAccessor> {
    public fun isGetter(): Boolean
    public fun hasBody(): Boolean
    public fun hasBlockBody(): Boolean
}

public interface KotlinPropertyStub : KotlinCallableStubBase<KtProperty> {
    public fun isVar(): Boolean
    public fun hasDelegate(): Boolean
    public fun hasDelegateExpression(): Boolean
    public fun hasInitializer(): Boolean
    public fun hasReturnTypeRef(): Boolean
}

public interface KotlinCallableStubBase<TDeclaration: KtCallableDeclaration> : KotlinStubWithFqName<TDeclaration> {
    public fun isTopLevel(): Boolean
    public fun isExtension(): Boolean
}

public interface KotlinTypeParameterStub : KotlinStubWithFqName<KtTypeParameter> {
    public fun isInVariance(): Boolean
    public fun isOutVariance(): Boolean
}

public interface KotlinTypeProjectionStub : StubElement<KtTypeProjection> {
    public fun getProjectionKind(): KtProjectionKind
}

public interface KotlinUserTypeStub : StubElement<KtUserType> {
    public fun isAbsoluteInRootPackage(): Boolean
}
