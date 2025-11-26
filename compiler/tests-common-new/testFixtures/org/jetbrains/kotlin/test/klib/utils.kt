/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.StringDirective

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


context(directives: RegisteredDirectives, customCompilerVersion: LanguageVersion)
internal fun createUnmutingErrorIfNeeded(stringDirective: StringDirective): List<Throwable> {
    return if (directives[stringDirective].any { it.startsWith(customCompilerVersion.versionString) })
        listOf(
            AssertionError(
                "Looks like this test can be unmuted. Remove $customCompilerVersion from the $stringDirective directive"
            )
        )
    else emptyList()
}
