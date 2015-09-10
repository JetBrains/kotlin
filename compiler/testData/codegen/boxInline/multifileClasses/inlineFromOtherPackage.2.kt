@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

inline fun foo(body: () -> String): String = body()