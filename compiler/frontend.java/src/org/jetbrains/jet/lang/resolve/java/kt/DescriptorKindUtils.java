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

package org.jetbrains.jet.lang.resolve.java.kt;

import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;

/**
 * @author udalov
 */
public class DescriptorKindUtils {
    private static final int KIND_DECLARATION = 0;
    private static final int KIND_FAKE_OVERRIDE = 1;
    private static final int KIND_DELEGATION = 2;
    private static final int KIND_SYNTHESIZED = 3;

    private DescriptorKindUtils() {
    }

    public static int getDefaultKindValue() {
        return KIND_DECLARATION;
    }

    public static int kindToInt(CallableMemberDescriptor.Kind kind) {
        switch (kind) {
            case DECLARATION: return KIND_DECLARATION;
            case FAKE_OVERRIDE: return KIND_FAKE_OVERRIDE;
            case DELEGATION: return KIND_DELEGATION;
            case SYNTHESIZED: return KIND_SYNTHESIZED;
            default: throw new IllegalArgumentException("Unknown kind: " + kind);
        }
    }

    public static CallableMemberDescriptor.Kind intToKind(int value) {
        switch (value) {
            case KIND_DECLARATION: return CallableMemberDescriptor.Kind.DECLARATION;
            case KIND_FAKE_OVERRIDE: return CallableMemberDescriptor.Kind.FAKE_OVERRIDE;
            case KIND_DELEGATION: return CallableMemberDescriptor.Kind.DELEGATION;
            case KIND_SYNTHESIZED: return CallableMemberDescriptor.Kind.SYNTHESIZED;
            default: throw new IllegalArgumentException("Unknown int value of kind: " + value);
        }
    }
}
