/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

/**
 * Why [runId] can't name a run, or `null` if it can: it names a report file and a directory next to it,
 * so it must not contain path separators or be `.` or `..`.
 */
fun runIdError(runId: String): String? =
    if (runId.matches(RUN_ID_PATTERN) && runId != "." && runId != "..") null
    else "runId '$runId' must consist of letters, digits, '.', '_' and '-', and not be '.' or '..'"

private val RUN_ID_PATTERN = Regex("[A-Za-z0-9._-]+")
