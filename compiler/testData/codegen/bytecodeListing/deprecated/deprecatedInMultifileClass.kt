// WITH_STDLIB
// FILE: part.kt

@file:JvmMultifileClass
@file:JvmName("A")

package test

@Deprecated("")
val str: String
    get() = ""

@Deprecated("")
fun f() {}

@Deprecated("")
val Int.ext: Int get() = this

var Int.extA: Int
    get() = this
    @Deprecated("")
    set(value) {}
