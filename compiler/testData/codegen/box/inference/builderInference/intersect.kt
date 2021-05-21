// !LANGUAGE: +UnrestrictedBuilderInference
// WITH_RUNTIME

// FILE: Test.java

class Test {
    static <T> T foo(T x) { return x; }
}

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

@UseExperimental(ExperimentalTypeInference::class)
fun <R1> build(@BuilderInference block: TestInterface<R1>.() -> Unit) {}

@UseExperimental(ExperimentalTypeInference::class)
fun <R2> build2(@BuilderInference block: TestInterface<R2>.() -> Unit) {}

class In<in K>

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
    fun getIn(): In<R>
}

fun <U> id(x: U) = x
fun <E> intersect(vararg x: In<E>): E = null as E

fun box(): String {
    val ret = build {
        emit("1")
        intersect(getIn(), getIn())
        intersect(getIn(), Test.foo(getIn()))
        intersect(Test.foo(getIn()), Test.foo(getIn()))
        intersect(Test.foo(getIn()), getIn())

        build2 {
            emit(1)
            intersect(this@build.getIn(), getIn())
            intersect(getIn(), Test.foo(this@build.getIn()))
            intersect(Test.foo(this@build.getIn()), Test.foo(getIn()))
            intersect(Test.foo(getIn()), this@build.getIn())
            ""
        }
        ""
    }
    return "OK"
}
