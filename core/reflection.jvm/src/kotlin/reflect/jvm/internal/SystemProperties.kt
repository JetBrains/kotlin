/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

/**
 * True if the system property `kotlin.reflect.jvm.useK1Implementation` is set to true.
 *
 * This system property can be used to change kotlin-reflect implementation to the legacy one, based on parts of the K1 compiler,
 * in case of any problems with the new implementation, based on kotlin-metadata-jvm.
 *
 * Changing the value of the system property after it has been read (i.e., after any non-trivial operation in kotlin-reflect)
 * is not supported and might lead to unexpected or incorrect behavior.
 *
 * See KT-75463 and related issues for more information.
 */
internal var useK1Implementation = runCatching { // 'var' because the field is mutated by test infra via reflection
    System.getProperty("kotlin.reflect.jvm.useK1Implementation")
}.getOrNull()?.toBoolean() == true

/**
 * Fake overrides implementation for new kotlin-reflect is in progress. This feature flag turns it on
 */
internal var newFakeOverridesImplementation = runCatching { // 'var' because the field is mutated by test infra via reflection
    System.getProperty("kotlin.reflect.jvm.newFakeOverridesImplementation")
}.getOrNull()?.toBoolean() == true

/**
 * True if the system property `kotlin.reflect.jvm.loadMetadataDirectly` is set to true.
 *
 * This system property can be used to instruct the kotlin-reflect implementation to avoid using K1 compiler representation unless
 * absolutely required, until KT-75463 is resolved.
 *
 * Currently, kotlin-reflect still uses K1 compiler representation, "descriptors", to implement some API (e.g., `KClass.members`).
 * For most of the remaining API, new implementation is used, which is based on kotlin-metadata-jvm and Java reflection elements.
 * To prevent parsing Kotlin metadata twice in the worst case, the new implementation loads the parsed binary data from descriptors.
 * This is more efficient in case you use both `KClass.members` and something else, however it's less efficient if you don't use API that
 * is still implemented via descriptors.
 *
 * This system property instructs kotlin-reflect to load Kotlin metadata directly in the new implementation, avoiding descriptors.
 * This is more efficient if you only use API that is already implemented without descriptors. However, this is less efficient if API
 * implemented via descriptors (such as `KClass.members`) is used as well, because it will lead to second parsing of Kotlin metadata.
 *
 * Note: at the moment, using reflection for Kotlin built-in classes that are mapped to Java classes is not fully supported when this
 * property is enabled. Such classes will be seen as their corresponding Java counterparts.
 */
internal val loadMetadataDirectly = runCatching {
    System.getProperty("kotlin.reflect.jvm.loadMetadataDirectly")
}.getOrNull()?.toBoolean() == true
