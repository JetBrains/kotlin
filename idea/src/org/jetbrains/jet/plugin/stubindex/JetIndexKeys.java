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

package org.jetbrains.jet.plugin.stubindex;

import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.jet.lang.psi.*;

public interface JetIndexKeys {
    StubIndexKey<String, JetClassOrObject> SHORT_NAME_KEY = StubIndexKey.createIndexKey("jet.class.shortName");
    StubIndexKey<String, JetClassOrObject> SUPERCLASS_NAME_KEY = StubIndexKey.createIndexKey("jet.class.superClassName");
    StubIndexKey<String, JetClassOrObject> FQN_KEY = StubIndexKey.createIndexKey("jet.fqn");

    StubIndexKey<String, JetObjectDeclaration> TOP_LEVEL_OBJECT_SHORT_NAME_KEY =
            StubIndexKey.createIndexKey("jet.top.level.object.short.name");

    StubIndexKey<String, JetNamedFunction> TOP_LEVEL_FUNCTION_SHORT_NAME_KEY =
            StubIndexKey.createIndexKey("jet.top.level.function.short.name");

    StubIndexKey<String, JetNamedFunction> TOP_LEVEL_EXTENSION_FUNCTION_SHORT_NAME_KEY =
            StubIndexKey.createIndexKey("jet.top.level.extension.function.short.name");

    /** Stores package top level function (both extension and non-extension) full qualified names. */
    StubIndexKey<String, JetNamedFunction> TOP_LEVEL_FUNCTIONS_FQN_NAME_KEY =
            StubIndexKey.createIndexKey("jet.top.level.functions.fqn.name");

    StubIndexKey<String, JetProperty> TOP_LEVEL_PROPERTY_FQN_NAME_KEY =
            StubIndexKey.createIndexKey("jet.top.level.property.fqn.name");

    StubIndexKey<String, JetNamedFunction> FUNCTIONS_SHORT_NAME_KEY = StubIndexKey.createIndexKey("jet.functions.short.name");
    StubIndexKey<String, JetProperty> PROPERTIES_SHORT_NAME_KEY = StubIndexKey.createIndexKey("jet.properties.short.name");

    StubIndexKey<String, JetAnnotationEntry> ANNOTATIONS_KEY = StubIndexKey.createIndexKey("jet.annotations");
}

