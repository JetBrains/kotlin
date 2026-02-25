/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

internal fun <T> K2JVMCompilerArguments.applyProfileCompilerCommand(profileCompilerCommand: T?) {
    require(profileCompilerCommand is String?) { "X_PROFILE must be a string, but was $profileCompilerCommand" }

    this.profileCompilerCommand = profileCompilerCommand
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyProfileCompilerCommand(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String?) { "X_PROFILE home must be a string, but was $currentValue" }

    return compilerArgs.profileCompilerCommand as T
}

internal fun <T> K2JVMCompilerArguments.applyJdkHome(jdkHome: T?) {
    require(jdkHome is String?) { "JDK home must be a string, but was $jdkHome" }

    this.jdkHome = jdkHome
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyJdkHome(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String?) { "JDK home must be a string, but was $currentValue" }

    return compilerArgs.jdkHome as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applyAdditionalJavaModules(javaModules: T?) {
    require(javaModules is Array<*>? && javaModules?.isArrayOf<String>() == true) { "Xadd-modules must be an array of string, but was $jdkHome" }

    this.additionalJavaModules = javaModules as Array<String>?
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyAdditionalJavaModules(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is Array<*>? && currentValue?.isArrayOf<String>() == true) { "Xadd-modules must be an array of string, but was $currentValue" }

    return compilerArgs.jdkHome as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applyJvmDefaultStable(mode: T?) {
    require(mode is String?) { "jvm-default must be a string, but was $mode" }

    this.jvmDefaultStable = mode as String?
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyJvmDefaultStable(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String?) { "jvm-default must be a string, but was $currentValue" }

    return compilerArgs.jdkHome as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applyAbiStability(mode: T?) {
    require(mode is String?) { "Xabi-stability must be a string, but was $mode" }

    this.abiStability = mode as String?
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyAbiStability(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String?) { "Xabi-stability must be a string, but was $currentValue" }

    return compilerArgs.abiStability as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applyAssertionsMode(mode: T?) {
    require(mode is String?) { "Xassertions must be a string, but was $mode" }

    this.assertionsMode = mode as String?
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyAssertionsMode(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String?) { "Xassertions must be a string, but was $currentValue" }

    return compilerArgs.assertionsMode as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applyJspecifyAnnotations(mode: T?) {
    require(mode is String?) { "Xjspecify-annotations must be a string, but was $mode" }

    this.jspecifyAnnotations = mode as String?
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyJspecifyAnnotations(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String?) { "Xjspecify-annotations must be a string, but was $currentValue" }

    return compilerArgs.jspecifyAnnotations as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applyLambdas(mode: T?) {
    require(mode is String?) { "Xlambdas must be a string, but was $mode" }

    this.lambdas = mode as String?
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyLambdas(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String?) { "Xlambdas must be a string, but was $currentValue" }

    return compilerArgs.lambdas as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applySamConversions(mode: T?) {
    require(mode is String?) { "Xsam-conversions must be a string, but was $mode" }

    this.samConversions = mode as String
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applySamConversions(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String?) { "Xsam-conversions must be a string, but was $currentValue" }

    return compilerArgs.samConversions as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applyStringConcat(mode: T?) {
    require(mode is String?) { "Xstring-concat must be a string, but was $mode" }

    this.stringConcat = mode as String?
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyStringConcat(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String?) { "Xstring-concat must be a string, but was $currentValue" }

    return compilerArgs.stringConcat as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applySupportCompatqualCheckerFrameworkAnnotations(mode: T?) {
    require(mode is String?) { "Xsupport-compatqual-checker-framework-annotations must be a string, but was $mode" }

    this.supportCompatqualCheckerFrameworkAnnotations = mode as String?
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applySupportCompatqualCheckerFrameworkAnnotations(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String?) { "Xsupport-compatqual-checker-framework-annotations must be a string, but was $currentValue" }

    return compilerArgs.supportCompatqualCheckerFrameworkAnnotations as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applyWhenExpressionsGeneration(mode: T?) {
    require(mode is String?) { "Xwhen-expressions must be a string, but was $mode" }

    this.whenExpressionsGeneration = mode as String?
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyWhenExpressionsGeneration(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String?) { "Xwhen-expressions must be a string, but was $currentValue" }

    return compilerArgs.whenExpressionsGeneration as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applyFriendPaths(paths: T?) {
    require(paths is Array<*>? && paths?.all { it is String } == true) { "Xfriend-paths must be an array of strings, but was $paths" }

    this.friendPaths = paths as Array<String>?
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyFriendPaths(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is Array<*>? && currentValue?.all { it is String } == true) { "Xfriend-paths must be an array of strings, but was $currentValue" }

    return compilerArgs.friendPaths as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applyJavaSourceRoots(paths: T?) {
    require(paths is Array<*>? && paths?.all { it is String } == true) { "Xjava-source-roots must be an array of strings, but was $paths" }

    this.javaSourceRoots = paths as Array<String>?
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyJavaSourceRoots(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is Array<*>? && currentValue?.all { it is String } == true) { "Xjava-source-roots must be an array of strings, but was $currentValue" }

    return compilerArgs.javaSourceRoots as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applyKlibLibraries(paths: T?) {
    require(paths is String) { "Xklib must be a string, but was $paths" }

    this.klibLibraries = paths as String?
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyKlibLibraries(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String) { "Xklib must be a string, but was $currentValue" }

    return compilerArgs.klibLibraries as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applyClasspath(paths: T?) {
    require(paths is String) { "classpath must be a string, but was $paths" }

    this.classpath = paths as String?
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyClasspath(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String) { "classpath must be a string, but was $currentValue" }

    return compilerArgs.classpath as T
}

@Suppress("UNCHECKED_CAST")
internal fun <T> K2JVMCompilerArguments.applyJavaModulePath(paths: T?) {
    require(paths is String) { "Xmodule-path must be a string, but was $paths" }

    this.javaModulePath = paths as String?
}

@Suppress("UNCHECKED_CAST")
internal fun <T> applyJavaModulePath(
    currentValue: Any?,
    compilerArgs: K2JVMCompilerArguments,
): T {
    require(currentValue is String) { "Xmodule-path must be a string, but was $currentValue" }

    return compilerArgs.classpath as T
}