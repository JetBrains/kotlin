// TARGET_BACKEND: JVM_IR

public inline fun <reified T> myEmptyArray(): Array<T> = arrayOfNulls<T>(0) as Array<T>

inline fun <reified T> Array<out T>?.myOrEmpty(): Array<out T> = this ?: myEmptyArray<T>()

fun foo(a : Array<String>?) = a.myOrEmpty()

val a = arrayOf<Int>(1) as Array<Any>

val b = arrayOf<Int>(1) as Array<Int>

val c = arrayOf(arrayOf<Int>(1)) as Array<Array<Any>?>

// 0 CHECKCAST