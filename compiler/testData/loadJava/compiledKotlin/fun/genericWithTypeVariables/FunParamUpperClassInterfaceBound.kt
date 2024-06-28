// TARGET_BACKEND: JVM
package test

fun <A> tres(): Int where A : java.lang.Number, A : java.io.Serializable = 1
