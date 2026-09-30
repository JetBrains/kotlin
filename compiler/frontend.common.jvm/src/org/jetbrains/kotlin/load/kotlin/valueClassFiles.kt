/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.isJvmTargetValhallaCompatible
import org.jetbrains.org.objectweb.asm.Opcodes

/** `ACC_IDENTITY` of JEP 401 is `ACC_SUPER`. */
const val ACC_IDENTITY = Opcodes.ACC_SUPER

/**
 * Whether this class file declares a Valhalla value class (JEP 401): a preview class file of a JVM target with value classes, without
 * `ACC_IDENTITY`. A Kotlin value class compiled without Valhalla value classes is an identity class in its class file.
 */
fun FileBasedKotlinClass.isBinaryValhallaValueClass(): Boolean {
    val jvmTarget = JvmTarget.entries.find { it.majorVersion == classVersion.major } ?: return false
    return isJvmTargetValhallaCompatible(jvmTarget, classVersion.isPreview) && !hasIdentityFlag()
}

/**
 * Whether a full value class is compiled as an identity class: a Valhalla value class can't extend an identity class. For a class read
 * from a class file, its [binaryClass] tells. A class of this compilation is one without Valhalla value classes, or if it
 * [extendsValueClassCompiledAsIdentityClass].
 */
inline fun isValueClassCompiledAsIdentityClass(
    binaryClass: FileBasedKotlinClass?,
    isValhallaSupportEnabled: Boolean,
    extendsValueClassCompiledAsIdentityClass: () -> Boolean,
): Boolean =
    if (binaryClass != null) !binaryClass.isBinaryValhallaValueClass()
    else !isValhallaSupportEnabled || extendsValueClassCompiledAsIdentityClass()
