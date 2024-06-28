/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.annotations

import org.jetbrains.kotlin.descriptors.ClassKind
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

    TYPE_PROJECTION("type projection", false),
    STAR_PROJECTION("star projection", false),
    PROPERTY_PARAMETER("property constructor parameter", false),

    // includes only top level classes and nested/inner classes (but not enums, objects, interfaces and local classes)
    CLASS_ONLY("class", false),

    // does not include OBJECT_LITERAL but DOES include both STANDALONE_OBJECT and COMPANION_OBJECT
    OBJECT("object", false),
    STANDALONE_OBJECT("standalone object", false),
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

    BACKING_FIELD("backing field"),

    INITIALIZER("initializer", false),
    DESTRUCTURING_DECLARATION("destructuring declaration", false),
    LAMBDA_EXPRESSION("lambda expression", false),
    ANONYMOUS_FUNCTION("anonymous function", false),
    OBJECT_LITERAL("object literal", false),
    ;

    companion object {

        private val map = HashMap<String, KotlinTarget>()

        init {
            for (target in entries) {
                map[target.name] = target
            }
        }

        fun valueOrNull(name: String): KotlinTarget? = map[name]

        val DEFAULT_TARGET_SET: Set<KotlinTarget> = entries.filter { it.isDefault }.toSet()
        val ALL_TARGET_SET: Set<KotlinTarget> = entries.toSet()

        val ANNOTATION_CLASS_LIST = listOf(ANNOTATION_CLASS, CLASS)
        val LOCAL_CLASS_LIST = listOf(LOCAL_CLASS, CLASS)
        val CLASS_LIST = listOf(CLASS_ONLY, CLASS)
        val COMPANION_OBJECT_LIST = listOf(COMPANION_OBJECT, OBJECT, CLASS)
        val OBJECT_LIST = listOf(STANDALONE_OBJECT, OBJECT, CLASS)
        val INTERFACE_LIST = listOf(INTERFACE, CLASS)
        val ENUM_LIST = listOf(ENUM_CLASS, CLASS)
        val ENUM_ENTRY_LIST = listOf(ENUM_ENTRY, PROPERTY, FIELD)
        val PROPERTY_SETTER_LIST = listOf(PROPERTY_SETTER)
        val PROPERTY_GETTER_LIST = listOf(PROPERTY_GETTER)
        val FUNCTION_LIST = listOf(FUNCTION)
        val FILE_LIST = listOf(FILE)

        fun classActualTargets(
            kind: ClassKind,
            isInnerClass: Boolean,
            isCompanionObject: Boolean,
            isLocalClass: Boolean
        ): List<KotlinTarget> = when (kind) {
            ClassKind.ANNOTATION_CLASS -> ANNOTATION_CLASS_LIST
            ClassKind.CLASS ->
                // inner local classes should be CLASS_ONLY, not LOCAL_CLASS
                if (!isInnerClass && isLocalClass) {
                    LOCAL_CLASS_LIST
                } else {
                    CLASS_LIST
                }
            ClassKind.OBJECT ->
                if (isCompanionObject) {
                    COMPANION_OBJECT_LIST
                } else {
                    OBJECT_LIST
                }
            ClassKind.INTERFACE -> INTERFACE_LIST
            ClassKind.ENUM_CLASS ->
                if (isLocalClass) {
                    LOCAL_CLASS_LIST
                } else {
                    ENUM_LIST
                }
            ClassKind.ENUM_ENTRY -> ENUM_ENTRY_LIST
        }

        val USE_SITE_MAPPING: Map<AnnotationUseSiteTarget, KotlinTarget> = mapOf(
            AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER to VALUE_PARAMETER,
            AnnotationUseSiteTarget.FIELD to FIELD,
            AnnotationUseSiteTarget.PROPERTY to PROPERTY,
            AnnotationUseSiteTarget.FILE to FILE,
            AnnotationUseSiteTarget.PROPERTY_GETTER to PROPERTY_GETTER,
            AnnotationUseSiteTarget.PROPERTY_SETTER to PROPERTY_SETTER,
            AnnotationUseSiteTarget.RECEIVER to VALUE_PARAMETER,
            AnnotationUseSiteTarget.SETTER_PARAMETER to VALUE_PARAMETER,
            AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD to FIELD
        )
    }
}
