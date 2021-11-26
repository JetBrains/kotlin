// !LANGUAGE: +UnrestrictedBuilderInference
// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

class Foo<K11, K22>(val key: K11) {
    fun emit1(key: K11) {}
    fun get(): K11 = null as K11
    fun emit2(key: K22) {}
}

@OptIn(ExperimentalTypeInference::class)
fun <K1, K2> build(@kotlin.BuilderInference builder: Foo<K1, K2>.() -> Unit) {}

fun run(x: Int) {
    build {
        emit1(x)
        emit2(get()) // We shouldn't report type mismatch, instead we should add constraint StubTypeVariable(K2) >: StubTypeVariable(K1), then infer K2 into Int among K1
    }
}

fun box(): String {
    run(1)
    return "OK"
}