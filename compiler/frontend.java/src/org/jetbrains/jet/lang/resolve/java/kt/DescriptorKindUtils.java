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

package org.jetbrains.jet.lang.resolve.java.kt;

import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;

public class DescriptorKindUtils {
    private DescriptorKindUtils() {
    }

    public static int kindToFlags(CallableMemberDescriptor.Kind kind) {
        switch (kind) {
            case DECLARATION: return JvmStdlibNames.FLAG_METHOD_KIND_DECLARATION;
            case FAKE_OVERRIDE: return JvmStdlibNames.FLAG_METHOD_KIND_FAKE_OVERRIDE;
            case DELEGATION: return JvmStdlibNames.FLAG_METHOD_KIND_DELEGATION;
            case SYNTHESIZED: return JvmStdlibNames.FLAG_METHOD_KIND_SYNTHESIZED;
            default: throw new IllegalArgumentException("Unknown kind: " + kind);
        }
    }

    public static CallableMemberDescriptor.Kind flagsToKind(int value) {
        switch (value & JvmStdlibNames.FLAG_METHOD_KIND_MASK) {
            case JvmStdlibNames.FLAG_METHOD_KIND_DECLARATION: return CallableMemberDescriptor.Kind.DECLARATION;
            case JvmStdlibNames.FLAG_METHOD_KIND_FAKE_OVERRIDE: return CallableMemberDescriptor.Kind.FAKE_OVERRIDE;
            case JvmStdlibNames.FLAG_METHOD_KIND_DELEGATION: return CallableMemberDescriptor.Kind.DELEGATION;
            case JvmStdlibNames.FLAG_METHOD_KIND_SYNTHESIZED: return CallableMemberDescriptor.Kind.SYNTHESIZED;
            default: throw new IllegalArgumentException("Unknown int value of kind: " + value);
        }
    }
}
