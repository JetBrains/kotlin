@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

inline fun foo(body: () -> String): String = bar(body())

public fun bar(x: String): String = x

inline fun <reified T> inlineOnly(x: Any?): Boolean = x is T