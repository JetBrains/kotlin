// FILE: MultifileClass.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package test

fun p1Fun() {}
fun String.p1ExtFun() {}
fun p1ExprFun(): Int = 0
fun p1FunWithParams(x: Int): Int { return x }

val p1Val: Int = 0
val String.p1ExtVal: Int get() = 0
var p1Var: Int = 0

@Deprecated("deprecated")
const val annotatedConstVal = 42

// FILE: MultifileClass__Part.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package test

fun p3Fun() {}
fun String.p3ExtFun() {}
val p3Val: Int = 0
val String.p3ExtVal: Int get() = 0

// FILE: part.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package test

fun p2Fun() {}
fun String.p2ExtFun() {}
val p2Val: Int = 0
val String.p2ExtVal: Int get() = 0