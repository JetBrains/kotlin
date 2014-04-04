/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.psi.tree.TokenSet;
import org.jetbrains.jet.lang.psi.*;

public interface JetStubElementTypes {
    JetFileElementType FILE = new JetFileElementType();

    JetClassElementType CLASS = new JetClassElementType("CLASS");
    JetFunctionElementType FUNCTION = new JetFunctionElementType("FUN");
    JetPropertyElementType PROPERTY = new JetPropertyElementType("PROPERTY");
    JetClassElementType ENUM_ENTRY = new JetClassElementType("ENUM_ENTRY");
    JetObjectElementType OBJECT_DECLARATION = new JetObjectElementType("OBJECT_DECLARATION");
    JetPlaceHolderStubElementType<JetClassObject> CLASS_OBJECT =
            new JetPlaceHolderStubElementType<JetClassObject>("CLASS_OBJECT", JetClassObject.class);
    JetPlaceHolderStubElementType<JetClassInitializer> ANONYMOUS_INITIALIZER =
            new JetPlaceHolderStubElementType<JetClassInitializer>("ANONYMOUS_INITIALIZER", JetClassInitializer.class);

    JetParameterElementType VALUE_PARAMETER = new JetParameterElementType("VALUE_PARAMETER");
    JetPlaceHolderStubElementType<JetParameterList> VALUE_PARAMETER_LIST =
            new JetPlaceHolderStubElementType<JetParameterList>("VALUE_PARAMETER_LIST", JetParameterList.class);

    JetTypeParameterElementType TYPE_PARAMETER = new JetTypeParameterElementType("TYPE_PARAMETER");
    JetPlaceHolderStubElementType<JetTypeParameterList> TYPE_PARAMETER_LIST =
            new JetPlaceHolderStubElementType<JetTypeParameterList>("TYPE_PARAMETER_LIST", JetTypeParameterList.class);

    JetAnnotationEntryElementType ANNOTATION_ENTRY = new JetAnnotationEntryElementType("ANNOTATION_ENTRY");
    JetPlaceHolderStubElementType<JetAnnotation> ANNOTATION =
                new JetPlaceHolderStubElementType<JetAnnotation>("ANNOTATION", JetAnnotation.class);

    JetPlaceHolderStubElementType<JetClassBody> CLASS_BODY =
            new JetPlaceHolderStubElementType<JetClassBody>("CLASS_BODY", JetClassBody.class);

    JetPlaceHolderStubElementType<JetImportList> IMPORT_LIST =
            new JetPlaceHolderStubElementType<JetImportList>("IMPORT_LIST", JetImportList.class);

    JetImportDirectiveElementType IMPORT_DIRECTIVE = new JetImportDirectiveElementType("IMPORT_DIRECTIVE");

    JetModifierListElementType<JetDeclarationModifierList> MODIFIER_LIST =
            new JetModifierListElementType<JetDeclarationModifierList>("MODIFIER_LIST", JetDeclarationModifierList.class);

    JetModifierListElementType<JetPrimaryConstructorModifierList> PRIMARY_CONSTRUCTOR_MODIFIER_LIST =
            new JetModifierListElementType<JetPrimaryConstructorModifierList>("PRIMARY_CONSTRUCTOR_MODIFIER_LIST", JetPrimaryConstructorModifierList.class);

    JetPlaceHolderStubElementType<JetTypeConstraintList> TYPE_CONSTRAINT_LIST =
            new JetPlaceHolderStubElementType<JetTypeConstraintList>("TYPE_CONSTRAINT_LIST", JetTypeConstraintList.class);

    JetPlaceHolderStubElementType<JetNullableType> NULLABLE_TYPE =
              new JetPlaceHolderStubElementType<JetNullableType>("NULLABLE_TYPE", JetNullableType.class);

    JetPlaceHolderStubElementType<JetTypeReference> TYPE_REFERENCE =
                new JetPlaceHolderStubElementType<JetTypeReference>("TYPE_REFERENCE", JetTypeReference.class);;

    JetUserTypeElementType USER_TYPE = new JetUserTypeElementType("USER_TYPE");

    JetPlaceHolderStubElementType<JetFunctionType> FUNCTION_TYPE =
                new JetPlaceHolderStubElementType<JetFunctionType>("FUNCTION_TYPE", JetFunctionType.class);
    
    JetTypeProjectionElementType TYPE_PROJECTION = new JetTypeProjectionElementType("TYPE_PROJECTION");
    
    JetPlaceHolderStubElementType<JetFunctionTypeReceiver> FUNCTION_TYPE_RECEIVER =
            new JetPlaceHolderStubElementType<JetFunctionTypeReceiver>("FUNCTION_TYPE_RECEIVER", JetFunctionTypeReceiver.class);

    JetNameReferenceExpressionElementType REFERENCE_EXPRESSION = new JetNameReferenceExpressionElementType("REFERENCE_EXPRESSION");
    JetDotQualifiedExpressionElementType DOT_QUALIFIED_EXPRESSION = new JetDotQualifiedExpressionElementType("DOT_QUALIFIED_EXPRESSION");

    JetPlaceHolderStubElementType<JetTypeArgumentList> TYPE_ARGUMENT_LIST =
                new JetPlaceHolderStubElementType<JetTypeArgumentList>("TYPE_ARGUMENT_LIST", JetTypeArgumentList.class);

    JetPlaceHolderStubElementType<JetDelegationSpecifierList> DELEGATION_SPECIFIER_LIST =
            new JetPlaceHolderStubElementType<JetDelegationSpecifierList>("DELEGATION_SPECIFIER_LIST", JetDelegationSpecifierList.class);

    JetPlaceHolderStubElementType<JetDelegatorByExpressionSpecifier> DELEGATOR_BY =
            new JetPlaceHolderStubElementType<JetDelegatorByExpressionSpecifier>("DELEGATOR_BY", JetDelegatorByExpressionSpecifier.class);
    JetPlaceHolderStubElementType<JetDelegatorToSuperCall> DELEGATOR_SUPER_CALL =
            new JetPlaceHolderStubElementType<JetDelegatorToSuperCall>("DELEGATOR_SUPER_CALL", JetDelegatorToSuperCall.class);
    JetPlaceHolderStubElementType<JetDelegatorToSuperClass> DELEGATOR_SUPER_CLASS =
            new JetPlaceHolderStubElementType<JetDelegatorToSuperClass>("DELEGATOR_SUPER_CLASS", JetDelegatorToSuperClass.class);
    JetPlaceHolderStubElementType<JetDelegatorToThisCall> THIS_CALL =
            new JetPlaceHolderStubElementType<JetDelegatorToThisCall>("THIS_CALL", JetDelegatorToThisCall.class);

    JetPlaceHolderStubElementType<JetConstructorCalleeExpression> CONSTRUCTOR_CALLEE =
            new JetPlaceHolderStubElementType<JetConstructorCalleeExpression>("CONSTRUCTOR_CALLEE", JetConstructorCalleeExpression.class);

    TokenSet DECLARATION_TYPES =
              TokenSet.create(CLASS, OBJECT_DECLARATION, CLASS_OBJECT, FUNCTION, PROPERTY, ANONYMOUS_INITIALIZER, ENUM_ENTRY);

    TokenSet DELEGATION_SPECIFIER_TYPES = TokenSet.create(DELEGATOR_BY, DELEGATOR_SUPER_CALL, DELEGATOR_SUPER_CLASS, THIS_CALL);

    TokenSet TYPE_ELEMENT_TYPES = TokenSet.create(USER_TYPE, NULLABLE_TYPE, FUNCTION_TYPE);
}
