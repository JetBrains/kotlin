/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2WasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyCommonCompilerArguments

fun convertJsCliArgumentsToWasmCliArguments(from: K2JSCompilerArguments): K2WasmCompilerArguments {
    val to = K2WasmCompilerArguments()
    copyCommonCompilerArguments(from, to)

    to.cacheDirectory = from.cacheDirectory
    to.errorTolerancePolicy = from.errorTolerancePolicy
    to.friendModules = from.friendModules
    to.friendModulesDisabled = from.friendModulesDisabled
    to.includes = from.includes
    to.irBuildCache = from.irBuildCache
    to.dce = from.irDce
    to.dumpDeclarationIrSizesToFile = from.irDceDumpDeclarationIrSizesToFile
    to.dceDumpReachabilityInfoToFile = from.irDceDumpReachabilityInfoToFile
    to.dcePrintReachabilityInfo = from.irDcePrintReachabilityInfo
    to.dceRuntimeDiagnostic = from.irDceRuntimeDiagnostic
    to.produceWasm = from.irProduceJs
    to.produceKlibDir = from.irProduceKlibDir
    to.produceKlibFile = from.irProduceKlibFile
    to.propertyLazyInitialization = from.irPropertyLazyInitialization
    to.libraries = from.libraries
    to.moduleName = from.irModuleName ?: from.moduleName
    to.outputDir = from.outputDir
    to.partialLinkageLogLevel = from.partialLinkageLogLevel
    to.partialLinkageMode = from.partialLinkageMode
    to.sourceMap = from.sourceMap
    to.sourceMapBaseDirs = from.sourceMapBaseDirs
    to.sourceMapPrefix = from.sourceMapPrefix
    to.debug = from.wasmDebug
    to.enableArrayRangeChecks = from.wasmEnableArrayRangeChecks
    to.enableAsserts = from.wasmEnableAsserts
    to.generateWat = from.wasmGenerateWat
    to.kClassFqn = from.wasmKClassFqn

    return to
}