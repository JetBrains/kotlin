/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import java.util.*

/**
 * Represents a unique identifier for a specific project. In this context, a "project" refers to the top-level entity
 * or the main project itself, rather than a project's module. It's important to note that in certain build systems,
 * like Gradle, the term "project" may be used to refer to individual modules. However, in this interface, we are
 * explicitly using it to represent a complete project rather than its sub-modules.
 */
public sealed interface ProjectId {
    public data class ProjectUUID(public val uuid: UUID) : ProjectId
}