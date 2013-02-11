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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

public class JvmAbi {
    /**
     * This constant is used to identify binary format (class file) versions
     * If you change class file metadata format and/or naming conventions, please increase this number
     */
    public static final int VERSION = 2;

    public static final String TRAIT_IMPL_CLASS_NAME = "$TImpl";
    public static final String TRAIT_IMPL_SUFFIX = "$" + TRAIT_IMPL_CLASS_NAME;

    public static final String DEFAULT_PARAMS_IMPL_SUFFIX = "$default";
    public static final String GETTER_PREFIX = "get";
    public static final String SETTER_PREFIX = "set";

    public static final String CLASS_OBJECT_CLASS_NAME = "object";
    public static final String CLASS_OBJECT_SUFFIX = "$" + CLASS_OBJECT_CLASS_NAME;

    public static final String INSTANCE_FIELD = "$instance";
    public static final String RECEIVER_PARAMETER = "$receiver";

    public static final JvmClassName JETBRAINS_NOT_NULL_ANNOTATION =
            JvmClassName.byFqNameWithoutInnerClasses("org.jetbrains.annotations.NotNull");

    public static boolean isClassObjectFqName(@NotNull FqName fqName) {
        return fqName.lastSegmentIs(Name.identifier(CLASS_OBJECT_CLASS_NAME));
    }

    private JvmAbi() {
    }
}
