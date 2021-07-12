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

package org.jetbrains.kotlin.resolve.jvm.jvmSignature;

public enum JvmMethodParameterKind {
    VALUE,
    THIS,
    OUTER,
    RECEIVER,
    CONTEXT_RECEIVER,
    CAPTURED_LOCAL_VARIABLE,
    ENUM_NAME_OR_ORDINAL,
    SUPER_CALL_PARAM,
    CONSTRUCTOR_MARKER;

    public boolean isSkippedInGenericSignature() {
        return this == OUTER || this == ENUM_NAME_OR_ORDINAL;
    }
}
