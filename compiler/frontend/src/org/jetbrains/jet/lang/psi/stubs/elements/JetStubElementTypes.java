/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

public interface JetStubElementTypes {
    JetFileElementType FILE = new JetFileElementType();

    JetClassElementType CLASS = new JetClassElementType("CLASS");
    JetFunctionElementType FUNCTION = new JetFunctionElementType("FUN");
    JetPropertyElementType PROPERTY = new JetPropertyElementType("PROPERTY");
    JetClassElementType ENUM_ENTRY = new JetClassElementType("ENUM_ENTRY");
    JetObjectElementType OBJECT_DECLARATION = new JetObjectElementType("OBJECT_DECLARATION");

    JetParameterElementType VALUE_PARAMETER = new JetParameterElementType("VALUE_PARAMETER");
    JetParameterListElementType VALUE_PARAMETER_LIST = new JetParameterListElementType("VALUE_PARAMETER_LIST");

    JetTypeParameterElementType TYPE_PARAMETER = new JetTypeParameterElementType("TYPE_PARAMETER");
    JetTypeParameterListElementType TYPE_PARAMETER_LIST = new JetTypeParameterListElementType("TYPE_PARAMETER_LIST");
    JetAnnotationElementType ANNOTATION_ENTRY = new JetAnnotationElementType("ANNOTATION_ENTRY");
}
