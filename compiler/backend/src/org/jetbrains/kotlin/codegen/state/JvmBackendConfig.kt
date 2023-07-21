/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.utils.metadataVersion

class JvmBackendConfig(configuration: CompilerConfiguration) {
    val languageVersionSettings: LanguageVersionSettings = configuration.languageVersionSettings

    val target: JvmTarget =
        configuration.get(JVMConfigurationKeys.JVM_TARGET) ?: JvmTarget.DEFAULT

    val useOldManglingSchemeForFunctionsWithInlineClassesInSignatures: Boolean =
        configuration.getBoolean(JVMConfigurationKeys.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME) ||
                languageVersionSettings.languageVersion.run { major == 1 && minor < 4 }

    val runtimeStringConcat: JvmStringConcat =
        if (target.majorVersion >= JvmTarget.JVM_9.majorVersion)
            configuration.get(JVMConfigurationKeys.STRING_CONCAT) ?: JvmStringConcat.INDY_WITH_CONSTANTS
        else JvmStringConcat.INLINE

    val samConversionsScheme: JvmClosureGenerationScheme =
        configuration.get(JVMConfigurationKeys.SAM_CONVERSIONS)
            ?: if (languageVersionSettings.supportsFeature(LanguageFeature.SamWrapperClassesAreSynthetic))
                JvmClosureGenerationScheme.INDY
            else
                JvmClosureGenerationScheme.CLASS

    val lambdasScheme: JvmClosureGenerationScheme =
        configuration.get(JVMConfigurationKeys.LAMBDAS)
            ?: if (languageVersionSettings.supportsFeature(LanguageFeature.LightweightLambdas))
                JvmClosureGenerationScheme.INDY
            else JvmClosureGenerationScheme.CLASS

    val useKotlinNothingValueException: Boolean =
        languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4 &&
                !configuration.getBoolean(JVMConfigurationKeys.NO_KOTLIN_NOTHING_VALUE_EXCEPTION)

    // In 1.6, `typeOf` became stable and started to rely on a few internal stdlib functions which were missing before 1.6.
    val stableTypeOf: Boolean =
        languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_6

    val generateOptimizedCallableReferenceSuperClasses: Boolean =
        languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4 &&
                !configuration.getBoolean(JVMConfigurationKeys.NO_OPTIMIZED_CALLABLE_REFERENCES)

    val isCallAssertionsDisabled: Boolean = configuration.getBoolean(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS)
    val isReceiverAssertionsDisabled: Boolean =
        configuration.getBoolean(JVMConfigurationKeys.DISABLE_RECEIVER_ASSERTIONS) ||
                !languageVersionSettings.supportsFeature(LanguageFeature.NullabilityAssertionOnExtensionReceiver)
    val isParamAssertionsDisabled: Boolean = configuration.getBoolean(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS)

    val assertionsMode: JVMAssertionsMode = configuration.get(JVMConfigurationKeys.ASSERTIONS_MODE, JVMAssertionsMode.DEFAULT)
    val isInlineDisabled: Boolean = configuration.getBoolean(CommonConfigurationKeys.DISABLE_INLINE)
    val useTypeTableInSerializer: Boolean = configuration.getBoolean(JVMConfigurationKeys.USE_TYPE_TABLE)

    val unifiedNullChecks: Boolean =
        languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4 &&
                !configuration.getBoolean(JVMConfigurationKeys.NO_UNIFIED_NULL_CHECKS)

    val noSourceCodeInNotNullAssertionExceptions: Boolean =
        (languageVersionSettings.supportsFeature(LanguageFeature.NoSourceCodeInNotNullAssertionExceptions)
                // This check is needed because we generate calls to `Intrinsics.checkNotNull` which is only available since 1.4
                // (when unified null checks were introduced).
                && unifiedNullChecks)
                // Never generate source code in assertion exceptions in K2 to make behavior of FIR PSI & FIR light-tree equivalent
                // (obtaining source code is not supported in light tree).
                || languageVersionSettings.languageVersion.usesK2

    val generateSmapCopyToAnnotation: Boolean = !configuration.getBoolean(JVMConfigurationKeys.NO_SOURCE_DEBUG_EXTENSION)

    val functionsWithInlineClassReturnTypesMangled: Boolean =
        languageVersionSettings.supportsFeature(LanguageFeature.MangleClassMembersReturningInlineClasses)

    val shouldValidateIr: Boolean = configuration.getBoolean(JVMConfigurationKeys.VALIDATE_IR)
    val shouldValidateBytecode: Boolean = configuration.getBoolean(JVMConfigurationKeys.VALIDATE_BYTECODE)

    val classFileVersion: Int = run {
        val minorVersion = if (configuration.getBoolean(JVMConfigurationKeys.ENABLE_JVM_PREVIEW)) 0xffff else 0
        (minorVersion shl 16) + target.majorVersion
    }

    val generateParametersMetadata: Boolean = configuration.getBoolean(JVMConfigurationKeys.PARAMETERS_METADATA)

    val shouldInlineConstVals: Boolean = languageVersionSettings.supportsFeature(LanguageFeature.InlineConstVals)

    val jvmDefaultMode: JvmDefaultMode = languageVersionSettings.getFlag(JvmAnalysisFlags.jvmDefaultMode)

    val disableOptimization: Boolean = configuration.getBoolean(JVMConfigurationKeys.DISABLE_OPTIMIZATION)

    val metadataVersion: BinaryVersion = configuration.metadataVersion()

    val abiStability: JvmAbiStability? = configuration.get(JVMConfigurationKeys.ABI_STABILITY)

    val noNewJavaAnnotationTargets: Boolean = configuration.getBoolean(JVMConfigurationKeys.NO_NEW_JAVA_ANNOTATION_TARGETS)

    val oldInnerClassesLogic: Boolean = configuration.getBoolean(JVMConfigurationKeys.OLD_INNER_CLASSES_LOGIC)
}
