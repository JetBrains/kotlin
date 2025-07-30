/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail

/**
 * Represents different kinds of Kotlin file stubs.
 *
 * The file can be:
 * * [file][WithPackage.File]
 * * [script][WithPackage.Script]
 * * [facade][WithPackage.Facade.Simple]
 * * [multifile class][WithPackage.Facade.MultifileClass]
 * * [invalid][Invalid]
 */
sealed interface KotlinFileStubKind {
    /**
     * A file that can hold a package.
     */
    sealed interface WithPackage : KotlinFileStubKind {
        /**
         * Represents a file that is a regular file.
         */
        @SubclassOptInRequired(KtImplementationDetail::class)
        interface File : WithPackage

        /**
         * The file package as [FqName].
         *
         * [FqName.ROOT] is used if the file has no explicit package.
         */
        val packageFqName: FqName

        /**
         * Represents a file that is a script.
         */
        @SubclassOptInRequired(KtImplementationDetail::class)
        interface Script : WithPackage

        /**
         * Represents a file facade.
         *
         * **Note**: [packageFqName] might be not the same as [FqName.parent] on [facadeFqName] due to [JvmPackageName].
         *
         * See [Package-level functions](https://kotlinlang.org/docs/java-to-kotlin-interop.html#package-level-functions).
         */
        sealed interface Facade : WithPackage {
            /**
             * The file facade class as [FqName].
             *
             * ### Example
             * `foo.kt`:
             * ```kotlin
             * package com.example
             *
             * fun baz() {}
             * ```
             *
             * Results to `com.example.FooKt`.
             *
             * `foo.kt`:
             * ```kotlin
             * @file:JvmName("Doo")
             * package com.example
             *
             * fun baz() {}
             * ```
             *
             * Results to `com.example.Doo`.
             *
             * `foo.kt`:
             * ```kotlin
             * @file:JvmName("Doo")
             * @file:JvmPackageName("another.pack")
             * package com.example
             *
             * fun baz() {}
             * ```
             * Results to `another.pack.Doo`.
             */
            val facadeFqName: FqName

            /**
             * Represents a simple class facade.
             */
            @SubclassOptInRequired(KtImplementationDetail::class)
            interface Simple : Facade {
                /**
                 * The simple class name of the facade.
                 *
                 * ### Example
                 * If the [facadeFqName] is `com.example.FooKt`, then this is `FooKt`.
                 */
                val partSimpleName: String
            }

            /**
             * Represents a multi-file class facade.
             *
             * @see JvmMultifileClass
             */
            @SubclassOptInRequired(KtImplementationDetail::class)
            interface MultifileClass : Facade {
                /**
                 * A simple names list of all the parts that are associated with a multi-file class facade.
                 * Simple names represent the unqualified names of each facade part.
                 *
                 * ### Example
                 * `MultifileClass.kt`:
                 * ```kotlin
                 * @file:[JvmName("MultifileClass") JvmMultifileClass]
                 * package test
                 *
                 * fun p1Fun() {}
                 *```
                 * `SecondPart.kt`:
                 * ```kotlin
                 * @file:[JvmName("MultifileClass") JvmMultifileClass]
                 * package test
                 *
                 * fun p2Fun() {}
                 *```
                 * [facadeFqName] is `test.MultifileClassKt`, [facadePartSimpleNames] is `["MultifileClass__MultifileClassKt", "MultifileClass__SecondPartKt"]`.
                 */
                val facadePartSimpleNames: List<String>
            }
        }
    }

    /**
     * Represents an invalid Kotlin file stub.
     *
     * For instance, if the file metadata is corrupted/incompatible.
     */
    @SubclassOptInRequired(KtImplementationDetail::class)
    interface Invalid : KotlinFileStubKind {
        /**
         * A human-readable error message that describes the reason why the file stub is invalid.
         * It is used instead of decompiled text.
         */
        val errorMessage: String
    }
}
