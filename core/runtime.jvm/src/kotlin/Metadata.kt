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

package kotlin

/**
 * This annotation is present on any class file produced by the Kotlin compiler and is read by the compiler and reflection.
 * Parameters have very short names on purpose: these names appear in the generated class files, and we'd like to reduce their size.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
internal annotation class Metadata(
        /**
         * A kind of the metadata this annotation encodes. Kotlin compiler recognizes the following kinds (see KotlinClassHeader.Kind):
         *
         * 1 Class
         * 2 File
         * 3 Synthetic class
         * 4 Multi-file class facade
         * 5 Multi-file class part
         *
         * The class file with a kind not listed here is treated as a non-Kotlin file.
         */
        val k: Int = 1,
        /**
         * The version of the metadata provided in the arguments of this annotation.
         */
        val mv: IntArray = intArrayOf(),
        /**
         * The version of the bytecode interface (naming conventions, signatures) of the class file annotated with this annotation.
         */
        val bv: IntArray = intArrayOf(),
        /**
         * Metadata in a custom format. The format may be different (or even absent) for different kinds.
         */
        val d1: Array<String> = arrayOf(),
        /**
         * An addition to [d1]: array of strings which occur in metadata, written in plain text so that strings already present
         * in the constant pool are reused. These strings may be then indexed in the metadata by an integer index in this array.
         */
        val d2: Array<String> = arrayOf(),
        /**
         * An extra string. For a multi-file part class, internal name of the facade class.
         */
        val xs: String = "",
        /**
         * An extra int. For multi-file class, represents code generation scheme.
         */
        val xi: Int = 0
)
