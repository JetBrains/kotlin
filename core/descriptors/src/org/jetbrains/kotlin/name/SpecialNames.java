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

package org.jetbrains.kotlin.name;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpecialNames {
    public static final Name NO_NAME_PROVIDED = Name.special("<no name provided>");
    public static final Name ROOT_PACKAGE = Name.special("<root package>");

    private static final String CLASS_OBJECT_FOR = "<class-object-for-";

    // This name is used as a key for the case when something has no name _due to a syntactic error_
    // Example: fun (x: Int) = 5
    //          There's no name for this function in the PSI
    // The name contains a GUID to avoid clashes, if a clash happens, it's not a big deal: the code does not compile anyway
    public static final Name SAFE_IDENTIFIER_FOR_NO_NAME = Name.identifier("no_name_in_PSI_3d19d79d_1ba9_4cd0_b7f5_b46aa3cd5d40");

    @NotNull
    public static Name safeIdentifier(@Nullable Name name) {
        return name != null && !name.isSpecial() ? name : SAFE_IDENTIFIER_FOR_NO_NAME;
    }

    @NotNull
    public static Name getClassObjectName(@NotNull Name className) {
        return Name.special(CLASS_OBJECT_FOR + className.asString() + ">");
    }

    public static boolean isClassObjectName(@NotNull Name name) {
        return name.isSpecial() && name.asString().startsWith(CLASS_OBJECT_FOR);
    }

    private SpecialNames() {}

}
