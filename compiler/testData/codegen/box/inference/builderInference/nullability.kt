// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -OPT_IN_IS_NOT_ENABLED
// WITH_STDLIB

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
}

@OptIn(ExperimentalTypeInference::class)
fun <R1> build(@BuilderInference block: TestInterface<R1>.() -> Unit) {}

@OptIn(ExperimentalTypeInference::class)
fun <R1 : Any> build2(@BuilderInference block: TestInterface<R1>.() -> Unit) {}

@OptIn(ExperimentalTypeInference::class)
fun <R1 : R2, R2 : Any> build3(@BuilderInference block: TestInterface<R1>.() -> Unit) {}

@OptIn(ExperimentalTypeInference::class)
fun <R1 : R2, R2> build4(x: R2, @BuilderInference block: TestInterface<R1>.() -> Unit) {}

fun test(a: String?) {
    val ret1 = build {
        emit("1")
//        get()?.equals("")
        val x = get()
//        x?.equals("")
        x ?: "1"
//        x!!
        ""
    }
//    val ret2 = build2 {
//        emit(1)
//        get()?.equals("")
//        val x = get()
//        x?.equals("")
//        x ?: 1
//        x!!
//        ""
//    }
//    val ret3 = build3 {
//        emit(1)
//        get()?.equals("")
//        val x = get()
//        x?.equals("")
//        x ?: 1
//        x!!
//        ""
//    }
//    val ret4 = build4(1) {
//        emit(1)
//        get()?.equals("")
//        val x = get()
//        x?.equals("")
//        x ?: 1
//        x!!
//        ""
//    }
}

fun box(): String {
    test("")
    return "OK"
}
