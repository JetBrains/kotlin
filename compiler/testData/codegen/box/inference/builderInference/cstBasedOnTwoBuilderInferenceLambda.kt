// WITH_STDLIB
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE
// FIR status: ARGUMENT_TYPE_MISMATCH at contribute arguments
// TARGET_BACKEND: JVM

import kotlin.experimental.ExperimentalTypeInference

class In<in K> {
    fun contribute(x: K) {}
}

class Out<out K> {
    fun get(): K = null as K
}

class Inv<K> {
    fun get(): K = null as K
}

interface A
class B: A
class C: A

@OptIn(ExperimentalTypeInference::class)
fun <K> build1(@BuilderInference builderAction1: In<K>.() -> Unit, @BuilderInference builderAction2: In<K>.() -> Unit): K = 1 as K

@OptIn(ExperimentalTypeInference::class)
fun <K> build2(@BuilderInference builderAction1: In<K>.() -> Unit, @BuilderInference builderAction2: In<K>.() -> Unit): K = B() as K

@OptIn(ExperimentalTypeInference::class)
fun <K> build3(@BuilderInference builderAction1: Out<K>.() -> Unit, @BuilderInference builderAction2: Out<K>.() -> Unit): K = 1 as K

@OptIn(ExperimentalTypeInference::class)
fun <K> build4(@BuilderInference builderAction1: Out<K>.() -> Unit, @BuilderInference builderAction2: Out<K>.() -> Unit): K = B() as K

@OptIn(ExperimentalTypeInference::class)
fun <K> build5(@BuilderInference builderAction1: Inv<K>.() -> Unit, @BuilderInference builderAction2: Inv<K>.() -> Unit): K = 1 as K

@OptIn(ExperimentalTypeInference::class)
fun <K> build6(@BuilderInference builderAction1: Inv<K>.() -> Unit, @BuilderInference builderAction2: Inv<K>.() -> Unit): K = B() as K

@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    val x1 = build1({ contribute(1f) }, { contribute(1.0) })
    <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<*> & Number}")!>x1<!>

    val y1 = build2({ contribute(B()) }, { contribute(C()) })
    <!DEBUG_INFO_EXPRESSION_TYPE("A")!>y1<!>

    val x2 = build3({ val x: Float = get() }, { val x: Double = get() })
    <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<*> & Number}")!>x2<!>

    val y2 = build4({ val x: B = get() }, { val x: C = get() })
    <!DEBUG_INFO_EXPRESSION_TYPE("A")!>y2<!>

    val x3 = build5({ val x: Float = get() }, { val x: Double = get() })
    <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<*> & Number}")!>x3<!>

    val y3 = build6({ val x: B = get() }, { val x: C = get() })
    <!DEBUG_INFO_EXPRESSION_TYPE("A")!>y3<!>

    return "OK"
}
