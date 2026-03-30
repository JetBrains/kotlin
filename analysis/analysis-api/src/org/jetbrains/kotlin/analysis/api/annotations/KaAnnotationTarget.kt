/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi

/**
 * Represents a code element kind which is a possible target of an annotation.
 *
 * [KaAnnotationTarget] corresponds to [kotlin.annotation.AnnotationTarget] from the standard library.
 *
 * @see org.jetbrains.kotlin.analysis.api.components.KaSymbolInformationProvider.applicableAnnotationTargets
 */
@KaExperimentalApi
public class KaAnnotationTarget private constructor(public val name: String) {
    @KaExperimentalApi
    public companion object {
        /**
         * A class, interface, or object, including annotation classes.
         */
        @JvmField
        public val CLASS: KaAnnotationTarget = KaAnnotationTarget("CLASS")

        /**
         * An annotation class only.
         */
        @JvmField
        public val ANNOTATION_CLASS: KaAnnotationTarget = KaAnnotationTarget("ANNOTATION_CLASS")

        /**
         * A generic type parameter.
         */
        @JvmField
        public val TYPE_PARAMETER: KaAnnotationTarget = KaAnnotationTarget("TYPE_PARAMETER")

        /**
         * A property.
         */
        @JvmField
        public val PROPERTY: KaAnnotationTarget = KaAnnotationTarget("PROPERTY")

        /**
         * A field, including a property's backing field.
         */
        @JvmField
        public val FIELD: KaAnnotationTarget = KaAnnotationTarget("FIELD")

        /**
         * A local variable.
         */
        @JvmField
        public val LOCAL_VARIABLE: KaAnnotationTarget = KaAnnotationTarget("LOCAL_VARIABLE")

        /**
         * A value parameter of a function or a constructor.
         */
        @JvmField
        public val VALUE_PARAMETER: KaAnnotationTarget = KaAnnotationTarget("VALUE_PARAMETER")

        /**
         * A constructor (primary or secondary).
         */
        @JvmField
        public val CONSTRUCTOR: KaAnnotationTarget = KaAnnotationTarget("CONSTRUCTOR")

        /**
         * A function (constructors are not included).
         */
        @JvmField
        public val FUNCTION: KaAnnotationTarget = KaAnnotationTarget("FUNCTION")

        /**
         * A property getter only.
         */
        @JvmField
        public val PROPERTY_GETTER: KaAnnotationTarget = KaAnnotationTarget("PROPERTY_GETTER")

        /**
         * A property setter only.
         */
        @JvmField
        public val PROPERTY_SETTER: KaAnnotationTarget = KaAnnotationTarget("PROPERTY_SETTER")

        /**
         * A type usage.
         */
        @JvmField
        public val TYPE: KaAnnotationTarget = KaAnnotationTarget("TYPE")

        /**
         * Any expression.
         */
        @JvmField
        public val EXPRESSION: KaAnnotationTarget = KaAnnotationTarget("EXPRESSION")

        /**
         * A file.
         */
        @JvmField
        public val FILE: KaAnnotationTarget = KaAnnotationTarget("FILE")

        /**
         * A type alias.
         */
        @JvmField
        public val TYPEALIAS: KaAnnotationTarget = KaAnnotationTarget("TYPEALIAS")
    }

    override fun toString(): String = name
}

