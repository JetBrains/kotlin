@file:JvmName("MultifileClass")
@file:JvmMultifileClass
package test

public val val1a = 42
private val String.val2a: Int get() = 0
public fun fn1a() {}
public fun String.fn2a() {}

class ShouldNotBeVisible1
interface ShouldNotBeVisible2

@Deprecated("deprecated")
const val annotatedConstVal = 42
