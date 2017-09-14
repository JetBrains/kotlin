/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
enum class KotlinTarget(val description: String, val isDefault: Boolean = true) {
    CLASS("class"),                            // includes CLASS_ONLY, OBJECT, COMPANION_OBJECT, OBJECT_LITERAL, INTERFACE, *_CLASS but not ENUM_ENTRY
    ANNOTATION_CLASS("annotation class"),
    TYPE_PARAMETER("type parameter", false),
    PROPERTY("property"),                      // includes *_PROPERTY (with and without backing field), PROPERTY_PARAMETER, ENUM_ENTRY
    FIELD("field"),                            // includes MEMBER_PROPERTY_WITH_FIELD, TOP_LEVEL_PROPERTY_WITH_FIELD, PROPERTY_PARAMETER, ENUM_ENTRY
    LOCAL_VARIABLE("local variable"),
    VALUE_PARAMETER("value parameter"),
    CONSTRUCTOR("constructor"),
    FUNCTION("function"),                      // includes *_FUNCTION and FUNCTION_LITERAL
    PROPERTY_GETTER("getter"),
    PROPERTY_SETTER("setter"),
    TYPE("type usage", false),
    EXPRESSION("expression", false),           // includes FUNCTION_LITERAL, OBJECT_LITERAL
    FILE("file", false),
    TYPEALIAS("typealias", false),

    RECEIVER("receiver", true),

    TYPE_PROJECTION("type projection", false),
    STAR_PROJECTION("star projection", false),
    PROPERTY_PARAMETER("property constructor parameter", false),

    CLASS_ONLY("class", false),  // includes only top level classes and nested/inner classes (but not enums, objects, interfaces and local classes)
    OBJECT("object", false),     // does not include OBJECT_LITERAL but DOES include COMPANION_OBJECT
    COMPANION_OBJECT("companion object", false),
    INTERFACE("interface", false),
    ENUM_CLASS("enum class", false),
    ENUM_ENTRY("enum entry", false),

    LOCAL_CLASS("local class", false),

    LOCAL_FUNCTION("local function", false),
    MEMBER_FUNCTION("member function", false),
    TOP_LEVEL_FUNCTION("top level function", false),

    MEMBER_PROPERTY("member property", false), // includes PROPERTY_PARAMETER, with and without field/delegate
    MEMBER_PROPERTY_WITH_BACKING_FIELD("member property with backing field", false),
    MEMBER_PROPERTY_WITH_DELEGATE("member property with delegate", false),
    MEMBER_PROPERTY_WITHOUT_FIELD_OR_DELEGATE("member property without backing field or delegate", false),
    TOP_LEVEL_PROPERTY("top level property", false), // with and without field/delegate
    TOP_LEVEL_PROPERTY_WITH_BACKING_FIELD("top level property with backing field", false),
    TOP_LEVEL_PROPERTY_WITH_DELEGATE("top level property with delegate", false),
    TOP_LEVEL_PROPERTY_WITHOUT_FIELD_OR_DELEGATE("top level property without backing field or delegate", false),

    INITIALIZER("initializer", false),
    DESTRUCTURING_DECLARATION("destructuring declaration", false),
    LAMBDA_EXPRESSION("lambda expression", false),
    ANONYMOUS_FUNCTION("anonymous function", false),
    OBJECT_LITERAL("object literal", false)
    ;

    companion object {

        private val map = HashMap<String, KotlinTarget>()

        init {
            for (target in KotlinTarget.values()) {
                map[target.name] = target
            }
        }

        fun valueOrNull(name: String): KotlinTarget? = map[name]

        val DEFAULT_TARGET_SET: Set<KotlinTarget> = values().filter { it.isDefault }.toSet()

        val ALL_TARGET_SET: Set<KotlinTarget> = values().toSet()

        fun classActualTargets(descriptor: ClassDescriptor): List<KotlinTarget> = when (descriptor.kind) {
            ClassKind.ANNOTATION_CLASS -> listOf(ANNOTATION_CLASS, CLASS)
            ClassKind.CLASS ->
                // inner local classes should be CLASS_ONLY, not LOCAL_CLASS
                if (!descriptor.isInner && DescriptorUtils.isLocal(descriptor)) {
                    listOf(LOCAL_CLASS, CLASS)
                }
                else {
                    listOf(CLASS_ONLY, CLASS)
                }
            ClassKind.OBJECT ->
                if (descriptor.isCompanionObject) {
                    listOf(COMPANION_OBJECT, OBJECT, CLASS)
                }
                else {
                    listOf(OBJECT, CLASS)
                }
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

        val USE_SITE_MAPPING: Map<AnnotationUseSiteTarget, KotlinTarget> = mapOf(
                AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER to VALUE_PARAMETER,
                AnnotationUseSiteTarget.FIELD to FIELD,
                AnnotationUseSiteTarget.PROPERTY to PROPERTY,
                AnnotationUseSiteTarget.FILE to FILE,
                AnnotationUseSiteTarget.PROPERTY_GETTER to PROPERTY_GETTER,
                AnnotationUseSiteTarget.PROPERTY_SETTER to PROPERTY_SETTER,
                AnnotationUseSiteTarget.RECEIVER to RECEIVER,
                AnnotationUseSiteTarget.SETTER_PARAMETER to VALUE_PARAMETER,
                AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD to FIELD)

    }
}
