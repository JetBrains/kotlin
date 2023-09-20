/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

object StandardTypes {
    val unit = type<Unit>()
    val nothing = type("kotlin", "Nothing")
    val boolean = type<Boolean>()
    val string = type<String>()
    val int = type<Int>()
    val array = type<Array<*>>()
    val list = type<List<*>>()
    val mutableList = type("kotlin.collections", "MutableList")
    val collection = type<Collection<*>>()
    val map = type<Map<*, *>>()
}