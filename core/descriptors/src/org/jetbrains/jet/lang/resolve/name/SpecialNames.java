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

package org.jetbrains.jet.lang.resolve.name;

import org.jetbrains.annotations.NotNull;

public class SpecialNames {
    public static final Name NO_NAME_PROVIDED = Name.special("<no name provided>");
    public static final Name ROOT_NAMESPACE = Name.special("<root namespace>");

    private static final String CLASS_OBJECT_FOR = "<class-object-for-";

    private SpecialNames() {}

    @NotNull
    public static Name getClassObjectName(@NotNull Name className) {
        return Name.special(CLASS_OBJECT_FOR + className.asString() + ">");
    }

    public static boolean isClassObjectName(@NotNull Name name) {
        return name.isSpecial() && name.asString().startsWith(CLASS_OBJECT_FOR);
    }
}
