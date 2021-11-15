// !DIAGNOSTICS: -UNUSED_PARAMETER -OPT_IN_IS_NOT_ENABLED
// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
class A1<T> {
    fun <BT1> builder1(@BuilderInference configure: A2<BT1>.() -> Unit) {}
}

@OptIn(ExperimentalTypeInference::class)
class A2<A2_BT1> {
    fun <BT2> builder2(@BuilderInference configure: A3<A2_BT1, BT2>.() -> Unit) {}
}

class A3<A3_BT1, A3_BT2> {
    fun resolver(x: A3_BT2) {}
}

fun foo(x: A1<String>) {
    x.builder1<String> {
        builder2 { resolver("") }
    }
}

fun box(): String {
    foo(A1<String>())
    return "OK"
}