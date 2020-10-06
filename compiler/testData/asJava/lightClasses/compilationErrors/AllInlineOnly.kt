// p.AllInlineOnly
// WITH_RUNTIME
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("AllInlineOnly")

package p

@kotlin.internal.InlineOnly
public inline fun f(): Int = 3

@kotlin.internal.InlineOnly
public inline fun g(p: String): String = "p"
