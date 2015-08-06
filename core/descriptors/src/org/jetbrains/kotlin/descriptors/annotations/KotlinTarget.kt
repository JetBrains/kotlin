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
    PACKAGE("package"),
    CLASSIFIER("classifier"),                  // includes CLASS, OBJECT, INTERFACE, *_CLASS but not ENUM_ENTRY
    ANNOTATION_CLASS("annotation class"),
    TYPE_PARAMETER("type parameter", false),
    PROPERTY("property"),                      // includes *_PROPERTY, PROPERTY_PARAMETER, ENUM_ENTRY
    FIELD("field"),
    LOCAL_VARIABLE("local variable"),
    VALUE_PARAMETER("value parameter"),
    CONSTRUCTOR("constructor"),
    FUNCTION("function"),                      // includes *_FUNCTION
    PROPERTY_GETTER("getter"),
    PROPERTY_SETTER("setter"),
    TYPE("type usage", false),
    EXPRESSION("expression", false),
    FILE("file", false),

    TYPE_PROJECTION("type projection", false),
    STAR_PROJECTION("star projection", false),
    PROPERTY_PARAMETER("property constructor parameter", false),

    CLASS("class", false),
    OBJECT("object", false),
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

    INITIALIZER("initializer", false)
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
            ClassKind.ANNOTATION_CLASS -> listOf(ANNOTATION_CLASS, CLASSIFIER)
            ClassKind.CLASS ->
                if (descriptor.isInner) {
                    listOf(INNER_CLASS, CLASSIFIER)
                }
                else if (DescriptorUtils.isLocal(descriptor)) {
                    listOf(LOCAL_CLASS, CLASSIFIER)
                }
                else {
                    listOf(CLASS, CLASSIFIER)
                }
            ClassKind.OBJECT -> listOf(OBJECT, CLASSIFIER)
            ClassKind.INTERFACE -> listOf(INTERFACE, CLASSIFIER)
            ClassKind.ENUM_CLASS ->
                if (DescriptorUtils.isLocal(descriptor)) {
                    listOf(LOCAL_CLASS, CLASSIFIER)
                }
                else {
                    listOf(ENUM_CLASS, CLASSIFIER)
                }
            ClassKind.ENUM_ENTRY -> listOf(ENUM_ENTRY, PROPERTY, FIELD)
        }
    }
}