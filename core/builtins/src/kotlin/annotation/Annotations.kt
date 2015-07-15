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

package kotlin.annotation

/**
 * Contains the list of code elements which are the possible annotation targets
 */
public enum class AnnotationTarget {
    /** Package directive */
    PACKAGE,
    /** Class, interface or object, annotation class is also included */
    CLASSIFIER,
    /** Annotation class only */
    ANNOTATION_CLASS,
    /** Generic type parameter (unsupported yet) */
    TYPE_PARAMETER,
    /** Property */
    PROPERTY,
    /** Field, including property's backing field */
    FIELD,
    /** Local variable */
    LOCAL_VARIABLE,
    /** Value parameter of a function or a constructor */
    VALUE_PARAMETER,
    /** Constructor only (primary or secondary) */
    CONSTRUCTOR,
    /** Function (constructors are not included) */
    FUNCTION,
    /** Property getter only */
    PROPERTY_GETTER,
    /** Property setter only */
    PROPERTY_SETTER,
    /** Type usage */
    TYPE,
    /** Any expression */
    EXPRESSION,
    /** File */
    FILE
}

/**
 * Contains the list of possible annotation's retentions.
 *
 * Determines how an annotation is stored in binary output.
 */
public enum class AnnotationRetention {
    /** Annotation isn't stored in binary output */
    SOURCE,
    /** Annotation is stored in binary output, but invisible for reflection */
    BINARY,
    /** Annotation is stored in binary output and visible for reflection (default retention) */
    RUNTIME
}

/**
 * This meta-annotation indicates the kinds of code elements which are possible targets of an annotation.
 *
 * If the target meta-annotation is not present on an annotation declaration, the annotation
 * is applicable to any code element, except type parameters, type usages, expressions, and files.
 *
 * @property allowedTargets list of allowed annotation targets
 */
target(AnnotationTarget.ANNOTATION_CLASS)
public annotation class target(vararg val allowedTargets: AnnotationTarget)

/**
 * This special meta-annotation is used to declare an annotation and to set its base properties.
 * So a class in Kotlin is an annotation if and only if it has the "annotation" meta-annotation.
 *
 * @property retention determines whether the annotation is stored in binary output and visible for reflection. By default, both are true.
 * @property repeatable true if annotation is repeatable (applicable twice or more on a single code element), otherwise false (default)
 */
target(AnnotationTarget.ANNOTATION_CLASS)
public annotation class annotation (
        val retention: AnnotationRetention = AnnotationRetention.RUNTIME,
        val repeatable: Boolean = false
)
