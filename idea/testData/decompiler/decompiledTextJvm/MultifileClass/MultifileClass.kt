@file:JvmName("MultifileClass")
@file:JvmMultifileClass
package test

private var i = 2

fun Int.plus(i: Int = 1) = this + i

class ShouldNotBeVisible1
interface ShouldNotBeVisible2
