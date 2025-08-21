/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.resolve.jvm.JvmClassName

/**
 * @property className Represents the class name.
 * @property jvmClassName Represent the internal JVM class name if its package is specified by @JvmPackageName, different from
 * [className] and it is not part of multifile class, null otherwise.
 * @property facadeClassName Represents internal JVM facade name if the class is a multifile class part, null otherwise.
 *
 * Declaration                  |className|jvmClassName|facadeClassName
 * -----------------------------|---------|------------|--------------
 * A.kt                         |AKt      |null        |null
 * -----------------------------|---------|------------|--------------
 * A.kt                         |AKt      |X/AKt       |null
 * @file:JvmPackageName("X")    |         |            |
 * -----------------------------|---------|------------|--------------
 * A.kt                         |F_AKt    |null        |F
 * @file:JvmMultifileClass      |         |            |
 * @file:JvmName("F")           |         |            |
 * -----------------------------|---------|------------|--------------
 * A.kt                         |X/F_AKt  |null        |X/F
 * @file:JvmPackageName("X")    |         |            |
 * @file:JvmMultifileClass      |         |            |
 * @file:JvmName("F")           |         |            |
 */
interface FacadeClassSource {
    val className: JvmClassName
    val jvmClassName: JvmClassName?
    val facadeClassName: JvmClassName?
}