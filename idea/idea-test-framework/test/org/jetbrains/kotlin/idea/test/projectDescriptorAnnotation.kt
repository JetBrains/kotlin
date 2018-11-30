/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ProjectDescriptorKind(val value: String)

const val JDK_AND_MULTIPLATFORM_STDLIB_WITH_SOURCES = "JDK_AND_MULTIPLATFORM_STDLIB_WITH_SOURCES"

const val KOTLIN_JVM_WITH_STDLIB_SOURCES = "KOTLIN_JVM_WITH_STDLIB_SOURCES"

const val KOTLIN_JAVASCRIPT = "KOTLIN_JAVASCRIPT"

const val KOTLIN_JVM_WITH_STDLIB_SOURCES_WITH_ADDITIONAL_JS = "KOTLIN_JVM_WITH_STDLIB_SOURCES_WITH_ADDITIONAL_JS"

const val KOTLIN_JAVASCRIPT_WITH_ADDITIONAL_JVM_WITH_STDLIB = "KOTLIN_JAVASCRIPT_WITH_ADDITIONAL_JVM_WITH_STDLIB"