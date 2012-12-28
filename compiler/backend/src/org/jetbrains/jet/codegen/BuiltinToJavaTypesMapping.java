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

package org.jetbrains.jet.codegen;

/**
 * Marks if backend's JetTypeMapper should map built-in types to Java types (e.g., jet.String into java.lang.String).
 * Disabling is needed for compiling builtins: we want types to be jet.* instead of java.util* to make it possible to load
 * builtins without JDK.
 */
public enum BuiltinToJavaTypesMapping {
    ENABLED,
    DISABLED
}
