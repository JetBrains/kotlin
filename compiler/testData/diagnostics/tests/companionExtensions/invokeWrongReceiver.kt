// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// RENDER_DIAGNOSTIC_ARGUMENTS
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-91409

fun test() {
    Function0()
}

companion operator fun <T> Function1.invoke(): (T) -> Unit = {}

companion fun Function0.a02() {}
fun t02() { Function1.a02() }

companion fun Function22.a04() {}
fun t04() { Function1.a04() }

companion fun kotlin.coroutines.SuspendFunction1.a06() {}
fun t06() { Function0.a06() }

companion fun Function1?.a38() {}
fun t38() { Function0.a38() }

typealias F17<A, B> = Function1<A, B>
companion fun F17.a17() {}
fun t17() { Function0.a17() }

companion val Function1.p16: Int get() = 1
fun t16() { Function0.p16 }

companion fun Function1.a11() {}
fun t11() { Function1.a11(1) }

companion fun Function1.a15(x: Int) {}
companion fun Function1.a15(x: String) {}
fun t15() { Function1.a15(1.0) }

companion fun Function1.a49(vararg x: Int) {}
companion fun Function1.a49(vararg x: Long) {}
fun t49() { Function1.a49() }

companion suspend fun Function1.a28() {}
fun t28() { Function1.a28() }

@Deprecated("x", level = DeprecationLevel.ERROR)
companion fun Function1.a30() {}
fun t30() { Function1.a30() }

companion fun Function1.a25() {}
companion fun Function2.a25() {}
fun t25() { Function0.a25() }

@Deprecated("x")
companion fun Function1.a12(): String = "ok"
fun m12() { Function1.a12() }

@Deprecated("x")
companion val Function1.p31: Int get() = 1
fun m31() { Function1.p31 }
