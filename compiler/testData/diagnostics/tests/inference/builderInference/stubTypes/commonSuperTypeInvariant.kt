// !LANGUAGE: +StableBuilderInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATION -EXPERIMENTAL_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_RUNTIME

// FILE: Test.java

class Test {
    static <T> T foo(T x) { return x; }
}

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

@UseExperimental(ExperimentalTypeInference::class)
fun <R1> build(@BuilderInference block: TestInterface<R1>.() -> Unit): R1 = TODO()

@UseExperimental(ExperimentalTypeInference::class)
fun <R2> build2(@BuilderInference block: TestInterface<R2>.() -> Unit): R2 = TODO()

class Inv<K>

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
    fun getInv(): Inv<R>
}

fun <U> id(x: U): U? = x
fun <E> select1(x: E, y: Inv<E>): E? = x
fun <E> select2(x: E, y: Inv<E?>): E = x
fun <E> select3(x: E?, y: Inv<E?>): E = x!!
fun <E> select4(x: E?, y: Inv<E>): E = x!!

fun test() {
    val ret1 = build {
        emit("1")
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)?")!>select1(get(), getInv())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)?")!>select1(get(), Test.foo(getInv()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)?")!>select1(Test.foo(get()), Test.foo(getInv()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)?")!>select1(Test.foo(get()), getInv())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)")!>select4(get(), getInv())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)")!>select4(get(), Test.foo(getInv()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)")!>select4(Test.foo(get()), Test.foo(getInv()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)")!>select4(Test.foo(get()), getInv())<!>

        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)")!>select4(id(Test.foo(get())), getInv())<!>

        build2 {
            emit(1)
            select1(this@build.get(), <!TYPE_MISMATCH("TypeVariable(R2); TypeVariable(R1)")!>getInv()<!>)
            select1(get(), <!TYPE_MISMATCH("TypeVariable(R1); TypeVariable(R2)")!>Test.foo(this@build.getInv())<!>)
            select1(Test.foo(this@build.get()), <!TYPE_MISMATCH("TypeVariable(R2); TypeVariable(R1)!")!>Test.foo(getInv())<!>)
            select1(Test.foo(get()), <!TYPE_MISMATCH("TypeVariable(R1); TypeVariable(R2)!")!>this@build.getInv()<!>)
            select4(this@build.get(), <!TYPE_MISMATCH("TypeVariable(R2); TypeVariable(R1)")!>getInv()<!>)
            select4(get(), <!TYPE_MISMATCH("TypeVariable(R1); TypeVariable(R2)")!>Test.foo(this@build.getInv())<!>)
            select4(Test.foo(this@build.get()), <!TYPE_MISMATCH("TypeVariable(R2); TypeVariable(R1)")!>Test.foo(getInv())<!>)
            select4(Test.foo(get()), <!TYPE_MISMATCH("TypeVariable(R1); TypeVariable(R2)")!>this@build.getInv()<!>)

            select4(id(Test.foo(this@build.get())), <!TYPE_MISMATCH("TypeVariable(R2); TypeVariable(R1)")!>getInv()<!>)
            ""
        }
        ""
    }

    val ret2 = build {
        emit(if (true) "1" else null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)")!>select2(get(), getInv())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)")!>select2(get(), Test.foo(getInv()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)")!>select2(Test.foo(get()), Test.foo(getInv()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)")!>select2(Test.foo(get()), getInv())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)")!>select3(get(), getInv())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)")!>select3(get(), Test.foo(getInv()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)")!>select3(Test.foo(get()), Test.foo(getInv()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R1)")!>select3(Test.foo(get()), getInv())<!>

        build2 {
            emit(1)
            select2(this@build.get(), <!TYPE_MISMATCH("TypeVariable(R2); TypeVariable(R1)")!>getInv()<!>)
            select2(get(), <!TYPE_MISMATCH("TypeVariable(R1); TypeVariable(R2)")!>Test.foo(this@build.getInv())<!>)
            select2(Test.foo(this@build.get()), <!TYPE_MISMATCH("TypeVariable(R2); TypeVariable(R1)!")!>Test.foo(getInv())<!>)
            select2(Test.foo(get()), <!TYPE_MISMATCH("TypeVariable(R1); TypeVariable(R2)!")!>this@build.getInv()<!>)
            select3(this@build.get(), <!TYPE_MISMATCH("TypeVariable(R2); TypeVariable(R1)")!>getInv()<!>)
            select3(get(), <!TYPE_MISMATCH("TypeVariable(R1); TypeVariable(R2)")!>Test.foo(this@build.getInv())<!>)
            select3(Test.foo(this@build.get()), <!TYPE_MISMATCH("TypeVariable(R2); TypeVariable(R1)")!>Test.foo(getInv())<!>)
            select3(Test.foo(get()), <!TYPE_MISMATCH("TypeVariable(R1); TypeVariable(R2)")!>this@build.getInv()<!>)
            ""
        }
        ""
    }
}
