/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support

/**
 * [EnforcedHostTarget] annotation is placed on a testrunner class to ignore target settings, and use the host as a target.
 */
@Target(AnnotationTarget.CLASS)
annotation class EnforcedHostTarget
