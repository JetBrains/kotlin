/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.abi

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import java.io.Serializable

/**
 * Set of filtering rules that restrict ABI declarations included in a dump.
 *
 * The rules combine inclusion and exclusion of declarations.
 * Each filter can be written as a filter for the class name (see [INCLUDE_NAMED] or [EXCLUDE_NAMED]), or an annotation filter (see [INCLUDE_ANNOTATED_WITH] or [EXCLUDE_ANNOTATED_WITH]).
 *
 * In order for a declaration (class, field, property, or function) to get into the dump, it must pass the inclusion **and** exclusion filters.
 *
 * A declaration passes the exclusion filters if it does not match any class names (see [EXCLUDE_NAMED]) or annotation  (see [EXCLUDE_ANNOTATED_WITH]) filter rules.
 *
 * A declaration passes the inclusion filters if there are no inclusion rules, or it matches any inclusion rule, or at least one of its members (actual for class declaration) matches any inclusion rule.
 *
 * @since 2.4.0
 */
@ExperimentalBuildToolsApi
public interface AbiFilters {

    /**
     * A builder for [AbiFilters].
     * Generates immutable instances of [AbiFilters] based on the configuration of this builder.
     *
     * @since 2.4.0
     */
    public interface Builder {

        /**
         * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
         *
         * @return the previously set value for an option
         * @throws IllegalStateException if the option was not set and has no default value
         *
         * @since 2.4.0
         */
        public operator fun <V> get(key: Option<V>): V

        /**
         * Set the [value] for option specified by [key], overriding any previous value for that option.
         *
         * @since 2.4.0
         */
        public operator fun <V> set(key: Option<V>, value: V)

        /**
         * Creates an immutable instance of [AbiFilters] based on the configuration of this builder.
         *
         * @since 2.4.0
         */
        public fun build(): AbiFilters
    }

    /**
     * An option for configuring a [AbiFilters].
     *
     * @see get
     * @see set
     * @see AbiFilters.Companion
     *
     * @since 2.4.0
     */
    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     *
     * @since 2.4.0
     */
    public operator fun <V> get(key: Option<V>): V

    public companion object {
        /**
         * Include a class, file-level property, or file-level function in a dump by its name.
         * Declarations that do not match the specified names, that do not have an annotation from [INCLUDE_ANNOTATED_WITH]
         * and do not have members marked with an annotation from [INCLUDE_ANNOTATED_WITH] are excluded from the dump.
         *
         * The name filter compares the qualified class name with the value in the filter:
         *
         * - For Kotlin declarations, fully qualified names are used.
         * It is important to keep in mind that dots are used everywhere as separators, even in the case of a nested class.
         * E.g. for qualified name `foo.bar.Container.Value`, here `Value` is a class nested in `Container`.
         *
         * - For classes from Java sources, canonical names are used.
         * The main motivation is a similar approach to writing the class name - dots are used everywhere as delimiters.
         *
         * Name templates are allowed, with support for wildcards such as `**`, `*`, and `?`:
         * - `**` - zero or any number of characters
         * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
         * - `?` - any single character.
         *
         * @since 2.4.0
         */
        @JvmField
        public val INCLUDE_NAMED: Option<Set<String>> = Option("INCLUDE_NAMED")

        /**
         * Excludes a class, file-level property, or file-level function from a dump by its name.
         *
         * The name filter compares the qualified class name with the value in the filter:
         *
         * - For Kotlin declarations, fully qualified names are used.
         * It is important to keep in mind that dots are used everywhere as separators, even in the case of a nested class.
         * E.g. for qualified name `foo.bar.Container.Value`, here `Value` is a class nested in `Container`.
         *
         * - For classes from Java sources, canonical names are used.
         * The main motivation is a similar approach to writing the class name - dots are used everywhere as delimiters.
         *
         * Name templates are allowed, with support for wildcards such as `**`, `*`, and `?`:
         * - `**` - zero or any number of characters
         * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
         * - `?` - any single character.
         *
         * @since 2.4.0
         */
        @JvmField
        public val EXCLUDE_NAMED: Option<Set<String>> = Option("EXCLUDE_NAMED")

        /**
         * Includes a declaration by annotations placed on it.
         *
         * Any declaration that is not marked with one of these annotations and does not match the [INCLUDE_NAMED] is excluded from the dump.
         *
         * The declaration can be a class, a class member (function or property), a top-level function or a top-level property.
         *
         * Name templates are allowed, with support for wildcards such as `**`, `*`, and `?`:
         * - `**` - zero or any number of characters
         * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
         * - `?` - any single character.
         *
         * The annotation should not have [Retention] equal to [AnnotationRetention.SOURCE], otherwise, filtering by it will not work.
         *
         * @since 2.4.0
         */
        @JvmField
        public val INCLUDE_ANNOTATED_WITH: Option<Set<String>> = Option("INCLUDE_ANNOTATED_WITH")

        /**
         * Excludes a declaration by annotations placed on it.
         *
         * It means that a class, a class member (function or property), a top-level function or a top-level property
         * marked by a specific annotation will be excluded from the dump.
         *
         * Name templates are allowed, with support for wildcards such as `**`, `*`, and `?`:
         * - `**` - zero or any number of characters
         * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
         * - `?` - any single character.
         *
         * The annotation should not have [Retention] equal to [AnnotationRetention.SOURCE], otherwise, filtering by it will not work.
         *
         * @since 2.4.0
         */
        @JvmField
        public val EXCLUDE_ANNOTATED_WITH: Option<Set<String>> = Option("EXCLUDE_ANNOTATED_WITH")
    }
}

/**
 * Target name consisting of two parts: a [customizedName] that could be configured by a user, and a [targetType]
 * that specifies a target platform and could not be configured by a user.
 *
 * @since 2.4.0
 */
@ExperimentalBuildToolsApi
public class KlibTargetId(
    /**
     * A type of klib target.
     */
    public val targetType: KlibTargetType,
    /**
     * A name of a target that could be configured by a user.
     * Usually, it's the same name as [KlibTargetType.canonicalName] from [targetType].
     */
    public val customizedName: String
): Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KlibTargetId) return false

        if (targetType != other.targetType) return false
        if (customizedName != other.customizedName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = targetType.hashCode()
        result = 31 * result + customizedName.hashCode()
        return result
    }
}


/**
 * A type of Kotlin target.
 * Specifies the platform or native architecture.
 *
 * @since 2.4.0
 */
@ExperimentalBuildToolsApi
public enum class KlibTargetType(public val canonicalName: String) {
    JS("js"),
    WASM_WASI("wasmWasi"),
    WASM_JS("wasmJs"),
    ANDROID_NATIVE_X64("androidNativeX64"),
    ANDROID_NATIVE_X86("androidNativeX86"),
    ANDROID_NATIVE_ARM32("androidNativeArm32"),
    ANDROID_NATIVE_ARM64("androidNativeArm64"),
    IOS_ARM64("iosArm64"),
    IOS_X64("iosX64"),
    IOS_SIMULATOR_ARM64("iosSimulatorArm64"),
    WATCHOS_ARM32("watchosArm32"),
    WATCHOS_ARM64("watchosArm64"),
    WATCHOS_X64("watchosX64"),
    WATCHOS_SIMULATOR_ARM64("watchosSimulatorArm64"),
    WATCHOS_DEVICE_ARM64("watchosDeviceArm64"),
    TVOS_ARM64("tvosArm64"),
    TVOS_X64("tvosX64"),
    TVOS_SIMULATOR_ARM64("tvosSimulatorArm64"),
    LINUX_X64("linuxX64"),
    MINGW_X64("mingwX64"),
    MACOS_X64("macosX64"),
    MACOS_ARM64("macosArm64"),
    LINUX_ARM64("linuxArm64"),
    IOS_ARM32("iosArm32"),
    WATCHOS_X86("watchosX86"),
    LINUX_ARM32_HFP("linuxArm32Hfp"),
    MINGW_X86("mingwX86");

    public companion object {
        /**
         * Gets a [KlibTargetType] instance from a Konan target name.
         *
         * @since 2.4.0
         */
        public fun fromKonanTargetName(konanName: String): KlibTargetType {
            return konanTargetMapping[konanName] ?: throw IllegalArgumentException("Konan name '$konanName' not found")
        }

        /**
         * Gets a [KlibTargetType] instance from a name used in ABI Validation dumps (canonicalName).
         *
         * @since 2.4.0
         */
        public fun fromCanonicalName(canonicalName: String): KlibTargetType {
            return values().firstOrNull {
                it.canonicalName == canonicalName
            } ?: throw IllegalArgumentException("Canonical name '$canonicalName' not found")
        }
    }
}

@ExperimentalBuildToolsApi
private val konanTargetMapping = mapOf(
    "android_x64" to KlibTargetType.ANDROID_NATIVE_X64,
    "android_x86" to KlibTargetType.ANDROID_NATIVE_X86,
    "android_arm32" to KlibTargetType.ANDROID_NATIVE_ARM32,
    "android_arm64" to KlibTargetType.ANDROID_NATIVE_ARM64,
    "ios_arm64" to KlibTargetType.IOS_ARM64,
    "ios_x64" to KlibTargetType.IOS_X64,
    "ios_simulator_arm64" to KlibTargetType.IOS_SIMULATOR_ARM64,
    "watchos_arm32" to KlibTargetType.WATCHOS_ARM32,
    "watchos_arm64" to KlibTargetType.WATCHOS_ARM64,
    "watchos_x64" to KlibTargetType.WATCHOS_X64,
    "watchos_simulator_arm64" to KlibTargetType.WATCHOS_SIMULATOR_ARM64,
    "watchos_device_arm64" to KlibTargetType.WATCHOS_DEVICE_ARM64,
    "tvos_arm64" to KlibTargetType.TVOS_ARM64,
    "tvos_x64" to KlibTargetType.TVOS_X64,
    "tvos_simulator_arm64" to KlibTargetType.TVOS_SIMULATOR_ARM64,
    "linux_x64" to KlibTargetType.LINUX_X64,
    "mingw_x64" to KlibTargetType.MINGW_X64,
    "macos_x64" to KlibTargetType.MACOS_X64,
    "macos_arm64" to KlibTargetType.MACOS_ARM64,
    "linux_arm64" to KlibTargetType.LINUX_ARM64,
    "ios_arm32" to KlibTargetType.IOS_ARM32,
    "watchos_x86" to KlibTargetType.WATCHOS_X86,
    "linux_arm32_hfp" to KlibTargetType.LINUX_ARM32_HFP,
    "mingw_x86" to KlibTargetType.MINGW_X86,
    "wasm-wasi" to KlibTargetType.WASM_WASI,
    "wasm-js" to KlibTargetType.WASM_JS
)
