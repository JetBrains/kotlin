/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ProjectDescriptorKind(val value: String)

const val JDK_AND_MULTIPLATFORM_STDLIB_WITH_SOURCES = "JDK_AND_MULTIPLATFORM_STDLIB_WITH_SOURCES"