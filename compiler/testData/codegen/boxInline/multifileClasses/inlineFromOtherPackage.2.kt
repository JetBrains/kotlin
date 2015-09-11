@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

inline fun foo(body: () -> String): String = bar(body())

public fun bar(x: String): String = x