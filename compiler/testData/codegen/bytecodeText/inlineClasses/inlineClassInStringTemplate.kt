// !LANGUAGE: +InlineClasses

// FILE: Z.kt
inline class Z(val value: Int)

// FILE: test.kt
fun test1_1(z: Z) = "$z"
fun test1_2(z: Z) = "$z$z"
fun test1_many(z: Z) = "$z $z $z"
fun test1_concat1(z: Z) = "-" + z
fun test1_concat2(z: Z) = "$z" + z
fun test1_concat3(z: Z) = "-" + z + z

fun test2_1(z: Z?) = "$z"
fun test2_2(z: Z?) = "$z$z"
fun test2_many(z: Z?) = "$z $z $z"
fun test2_concat1(z: Z?) = "-" + z
fun test2_concat2(z: Z?) = "$z" + z
fun test2_concat3(z: Z?) = "-" + z + z

// @TestKt.class:
// 0 box
// 0 unbox
