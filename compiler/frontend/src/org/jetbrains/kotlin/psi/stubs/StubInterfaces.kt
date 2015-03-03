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
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.psi.*

public trait KotlinFileStub : PsiFileStub<JetFile> {
    public fun getPackageFqName(): FqName
    public fun isScript(): Boolean
}

public trait KotlinPlaceHolderStub<T : JetElement> : StubElement<T>

public trait KotlinStubWithFqName<T : PsiNamedElement> : NamedStub<T> {
    public fun getFqName(): FqName?
}

public trait KotlinClassOrObjectStub<T : JetClassOrObject> : KotlinStubWithFqName<T> {
    public fun isLocal(): Boolean
    public fun getSuperNames(): List<String>
    public fun isTopLevel(): Boolean
}

public trait KotlinClassStub : KotlinClassOrObjectStub<JetClass> {
    public fun isTrait(): Boolean
    public fun isEnumEntry(): Boolean
}

public trait KotlinObjectStub : KotlinClassOrObjectStub<JetObjectDeclaration> {
    public fun isDefault(): Boolean
    public fun isObjectLiteral(): Boolean
}

public trait KotlinAnnotationEntryStub : StubElement<JetAnnotationEntry> {
    public fun getShortName(): String
    public fun hasValueArguments(): Boolean
}

public trait KotlinFunctionStub : KotlinStubWithFqName<JetNamedFunction> {
    public fun isTopLevel(): Boolean
    public fun isExtension(): Boolean
    public fun hasBlockBody(): Boolean
    public fun hasBody(): Boolean
    public fun hasTypeParameterListBeforeFunctionName(): Boolean
    public fun isProbablyNothingType(): Boolean
}

public trait KotlinImportDirectiveStub : StubElement<JetImportDirective> {
    public fun isAbsoluteInRootPackage(): Boolean
    public fun isAllUnder(): Boolean
    public fun getAliasName(): String?
    public fun isValid(): Boolean
}

public trait KotlinModifierListStub : StubElement<JetModifierList> {
    public fun hasModifier(modifierToken: JetModifierKeywordToken): Boolean
}

public trait KotlinNameReferenceExpressionStub : StubElement<JetNameReferenceExpression> {
    public fun getReferencedName(): String
}

public trait KotlinParameterStub : KotlinStubWithFqName<JetParameter> {
    public fun isMutable(): Boolean
    public fun hasValOrVarNode(): Boolean
    public fun hasDefaultValue(): Boolean
}

public trait KotlinPropertyAccessorStub : StubElement<JetPropertyAccessor> {
    public fun isGetter(): Boolean
    public fun hasBody(): Boolean
    public fun hasBlockBody(): Boolean
}

public trait KotlinPropertyStub : KotlinStubWithFqName<JetProperty> {
    public fun isVar(): Boolean
    public fun isTopLevel(): Boolean
    public fun hasDelegate(): Boolean
    public fun hasDelegateExpression(): Boolean
    public fun hasInitializer(): Boolean
    public fun hasReceiverTypeRef(): Boolean
    public fun hasReturnTypeRef(): Boolean
    public fun isProbablyNothingType(): Boolean
}

public trait KotlinTypeConstraintStub : StubElement<JetTypeConstraint> {
    public fun isDefaultObjectConstraint(): Boolean
}

public trait KotlinTypeParameterStub : KotlinStubWithFqName<JetTypeParameter> {
    public fun isInVariance(): Boolean
    public fun isOutVariance(): Boolean
}

public trait KotlinTypeProjectionStub : StubElement<JetTypeProjection> {
    public fun getProjectionKind(): JetProjectionKind
}

public trait KotlinUserTypeStub : StubElement<JetUserType> {
    public fun isAbsoluteInRootPackage(): Boolean
}
