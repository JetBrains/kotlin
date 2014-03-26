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

import org.jetbrains.jet.lang.psi.*;

public interface JetStubElementTypes {
    JetFileElementType FILE = new JetFileElementType();

    JetClassElementType CLASS = new JetClassElementType("CLASS");
    JetFunctionElementType FUNCTION = new JetFunctionElementType("FUN");
    JetPropertyElementType PROPERTY = new JetPropertyElementType("PROPERTY");
    JetClassElementType ENUM_ENTRY = new JetClassElementType("ENUM_ENTRY");
    JetObjectElementType OBJECT_DECLARATION = new JetObjectElementType("OBJECT_DECLARATION");

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

    JetModifierListElementType MODIFIER_LIST = new JetModifierListElementType("MODIFIER_LIST");
    JetModifierListElementType PRIMARY_CONSTRUCTOR_MODIFIER_LIST = new JetModifierListElementType("PRIMARY_CONSTRUCTOR_MODIFIER_LIST");

    JetPlaceHolderStubElementType<JetTypeConstraintList> TYPE_CONSTRAINT_LIST =
            new JetPlaceHolderStubElementType<JetTypeConstraintList>("TYPE_CONSTRAINT_LIST", JetTypeConstraintList.class);

    JetPlaceHolderStubElementType<JetNullableType> NULLABLE_TYPE =
              new JetPlaceHolderStubElementType<JetNullableType>("NULLABLE_TYPE", JetNullableType.class);

    JetPlaceHolderStubElementType<JetTypeReference> TYPE_REFERENCE =
                new JetPlaceHolderStubElementType<JetTypeReference>("TYPE_REFERENCE", JetTypeReference.class);;

    JetUserTypeElementType USER_TYPE = new JetUserTypeElementType("USER_TYPE");
}
