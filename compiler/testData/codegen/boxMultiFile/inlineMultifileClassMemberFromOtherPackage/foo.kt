@file:[JvmName("A") JvmMultifileClass]
package a

inline fun foo(body: () -> String): String = zee(body())