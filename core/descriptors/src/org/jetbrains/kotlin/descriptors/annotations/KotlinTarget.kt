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

package org.jetbrains.kotlin.descriptors.annotations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.util.*

// NOTE: this enum must have the same entries with kotlin.annotation.AnnotationTarget,
// and may also have some additional entries
public enum class KotlinTarget(val description: String, val isDefault: Boolean = true) {
    CLASS("class"),                            // includes CLASS_ONLY, OBJECT, OBJECT_LITERAL, INTERFACE, *_CLASS but not ENUM_ENTRY
    ANNOTATION_CLASS("annotation class"),
    TYPE_PARAMETER("type parameter", false),
    PROPERTY("property"),                      // includes *_PROPERTY, PROPERTY_PARAMETER, ENUM_ENTRY
    FIELD("field"),
    LOCAL_VARIABLE("local variable"),
    VALUE_PARAMETER("value parameter"),
    CONSTRUCTOR("constructor"),
    FUNCTION("function"),                      // includes *_FUNCTION and FUNCTION_LITERAL
    PROPERTY_GETTER("getter"),
    PROPERTY_SETTER("setter"),
    TYPE("type usage", false),
    EXPRESSION("expression", false),           // includes FUNCTION_LITERAL, OBJECT_LITERAL
    FILE("file", false),

    TYPE_PROJECTION("type projection", false),
    STAR_PROJECTION("star projection", false),
    PROPERTY_PARAMETER("property constructor parameter", false),

    CLASS_ONLY("class", false),  // includes only top level classes and nested classes (but not enums, objects, interfaces, inner or local classes)
    OBJECT("object", false),     // does not include OBJECT_LITERAL
    INTERFACE("interface", false),
    ENUM_CLASS("enum class", false),
    ENUM_ENTRY("enum entry", false),

    INNER_CLASS("inner class", false),
    LOCAL_CLASS("local class", false),

    LOCAL_FUNCTION("local function", false),
    MEMBER_FUNCTION("member function", false),
    TOP_LEVEL_FUNCTION("top level function", false),

    MEMBER_PROPERTY("member property", false), // includes PROPERTY_PARAMETER
    TOP_LEVEL_PROPERTY("top level property", false),

    INITIALIZER("initializer", false),
    MULTI_DECLARATION("multi declaration", false),
    FUNCTION_LITERAL("function literal", false),
    FUNCTION_EXPRESSION("function expression", false),
    OBJECT_LITERAL("object literal", false)
    ;

    companion object {

        private val map = HashMap<String, KotlinTarget>()

        init {
            for (target in KotlinTarget.values()) {
                map[target.name()] = target
            }
        }

        public fun valueOrNull(name: String): KotlinTarget? = map[name]

        public val DEFAULT_TARGET_SET: Set<KotlinTarget> = values().filter { it.isDefault }.toSet()

        public val ALL_TARGET_SET: Set<KotlinTarget> = values().toSet()

        public fun classActualTargets(descriptor: ClassDescriptor): List<KotlinTarget> = when (descriptor.kind) {
            ClassKind.ANNOTATION_CLASS -> listOf(ANNOTATION_CLASS, CLASS)
            ClassKind.CLASS ->
                if (descriptor.isInner) {
                    listOf(INNER_CLASS, CLASS)
                }
                else if (DescriptorUtils.isLocal(descriptor)) {
                    listOf(LOCAL_CLASS, CLASS)
                }
                else {
                    listOf(CLASS_ONLY, CLASS)
                }
            ClassKind.OBJECT -> listOf(OBJECT, CLASS)
            ClassKind.INTERFACE -> listOf(INTERFACE, CLASS)
            ClassKind.ENUM_CLASS ->
                if (DescriptorUtils.isLocal(descriptor)) {
                    listOf(LOCAL_CLASS, CLASS)
                }
                else {
                    listOf(ENUM_CLASS, CLASS)
                }
            ClassKind.ENUM_ENTRY -> listOf(ENUM_ENTRY, PROPERTY, FIELD)
        }

        public val USE_SITE_MAPPING: Map<AnnotationUseSiteTarget, KotlinTarget> = mapOf(
                AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER to VALUE_PARAMETER,
                AnnotationUseSiteTarget.FIELD to FIELD,
                AnnotationUseSiteTarget.PROPERTY to PROPERTY,
                AnnotationUseSiteTarget.FILE to FILE,
                AnnotationUseSiteTarget.PROPERTY_GETTER to PROPERTY_GETTER,
                AnnotationUseSiteTarget.PROPERTY_SETTER to PROPERTY_SETTER,
                AnnotationUseSiteTarget.RECEIVER to VALUE_PARAMETER,
                AnnotationUseSiteTarget.SETTER_PARAMETER to VALUE_PARAMETER)

    }
}