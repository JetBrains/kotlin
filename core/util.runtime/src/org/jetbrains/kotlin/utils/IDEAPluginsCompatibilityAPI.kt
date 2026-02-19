/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import kotlin.annotation.AnnotationTarget.*

/**
 * AS 211-based, IU211 or IC211 is the same _211 key.
 *
 * Usage in 213 platform means, that there was a **released** IDEA build or plugin on plugin marketplace compatible with
 * `213.*` platform that has used this API. If you found a usage in the IDEA and removed it usage before the first official release of the
 * corresponding platform it is considered as "not used" in this platform. In other words, if API was used in 221 EAP's
 * or even 221 RC but not in the 221 release -- then API don't have the 221 usage.
 */
enum class IDEAPlatforms {
    _211,
    _212,
    _213,
    _221,
    _222,
    _223,
    _233,
}

/**
 * This annotation is created to handle the following situation:
 * Sometimes IDE plugins are using some API from Kotlin plugin. But it is known that Kotlin plugin includes some parts of
 * the kotlin compiler. It means, that sometimes the IDE plugins are using API from kotlin compiler itself.
 * Also, we are providing updates of the Kotlin plugin with the new kotlin front-end inside.
 * But it also means, that because of such update some third-party plugins could be broken, because they have been using the API
 * from kotlin compiler that was removed or changed in the new version of the compiler.
 *
 * To prevent that we have some teamcity configuration that are checking such compatibility with all the plugins published into plugin side.
 * So, once we detected, that some API was broken, we should return corresponding API in the compiler and mark it.
 * Exactly for these usages this annotation should be used.
 *
 * List of IDEA platforms is needed to be able to remove the API completely at some point, once we'll stop support of the corresponding
 * IDEA platform.
 *
 * Instead of that we could use just Deprecation annotation with level Error, because it also preserves the binary compatibility and
 * enforce third-party plugin migrate to the new API, but unfortunately it isn't always possible. With OptIn Annotation that is more
 * flexible. And yes, please use Opt-in mechanic only as a last reserve.
 *
 * One of the legit use-cases for OptIn usage is the following:
 * Sometimes third-party plugins sources located inside IDEA monorepo. And in kt-*- branches we don't want to change the sources outside
 * the kotlin plugin, because it is incorrect. So instead of that we could add the corresponding OptIn in the corresponding module where
 * old API is used and **remove** such usages in the IDEA master.
 *
 * P.S. usedIn is declared as first parameter to force its usage -- without it is easy enough forget about it and declare only message
 *
 * [usedIn] see description for [IDEAPlatforms]
 * [message] -- you could add details what API should be used instead
 * [plugins] -- if you want, you could mention what IDEA plugins used these APIs. It is not mandatory, but sometimes could be helpful.
 */
@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPEALIAS)
@RequiresOptIn("This API will be removed from the Kotlin compiler, you shouldn't use it", level = RequiresOptIn.Level.ERROR)
annotation class IDEAPluginsCompatibilityAPI(vararg val usedIn: IDEAPlatforms, val message: String, val plugins: String = "")
