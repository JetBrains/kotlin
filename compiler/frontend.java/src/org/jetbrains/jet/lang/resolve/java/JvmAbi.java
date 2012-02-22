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

package org.jetbrains.jet.lang.resolve.java;

/**
 * @author Stepan Koltsov
 */
public class JvmAbi {
    public static final String TRAIT_IMPL_SUFFIX = "$$TImpl";
    public static final String DEFAULT_PARAMS_IMPL_SUFFIX = "$default";
    public static final String GETTER_PREFIX = "get";
    public static final String SETTER_PREFIX = "set";
    public static final String PACKAGE_CLASS = "namespace";
    public static final String CLASS_OBJECT_SUFFIX = "$ClassObject$";
    public static final String CLASS_OBJECT_CLASS_NAME = "ClassObject$";

    public static final JvmClassName JETBRAINS_NOT_NULL_ANNOTATION = new JvmClassName("org.jetbrains.annotations.NotNull");
}
