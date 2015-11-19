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

package org.jetbrains.kotlin.codegen.state

internal enum class TypeMappingMode(
        val needPrimitiveBoxing: Boolean = false,
        val isForAnnotationParameter: Boolean = false,
        val writeDeclarationSiteProjections: Boolean = true
) {
    /**
     * kotlin.Int is mapped to I
     */
    DEFAULT(),
    /**
     * kotlin.Int is mapped to Ljava/lang/Integer;
     */
    GENERIC_TYPE(needPrimitiveBoxing = true),
    /**
     * kotlin.Int is mapped to Ljava/lang/Integer;
     * No projections allowed in immediate arguments
     */
    SUPER_TYPE(needPrimitiveBoxing = true, writeDeclarationSiteProjections = false),
    /**
     * kotlin.reflect.KClass mapped to java.lang.Class
     * Other types mapped as DEFAULT
     */
    VALUE_FOR_ANNOTATION(isForAnnotationParameter = true),
    /**
     * kotlin.reflect.KClass mapped to java.lang.Class
     * Other types mapped as GENERIC_TYPE
     */
    GENERIC_TYPE_PARAMETER_FOR_ANNOTATION_PARAMETER(isForAnnotationParameter = true, needPrimitiveBoxing = true);
}
