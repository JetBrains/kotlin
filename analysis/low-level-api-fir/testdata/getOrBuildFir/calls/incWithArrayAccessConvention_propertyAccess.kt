/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

interface A {
    operator fun inc(): A
}

interface MyMap<K, V> {
    operator fun get(k: K): V
    operator fun set(k: K, v: V): Unit
}

fun test(m: MyMap<String, A>) {
    <expr>m</expr>["a"]++
}