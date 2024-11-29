// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

// FILE: JavaClass.java
public class JavaClass<T> {
    public String foo(){ return "foo";}
}

// FILE: test.kt

context(a: String)
fun test1() {
    a.length
}

context(a: () -> String)
fun test2() {
    a().length
}

context(a: T)
fun <T> test3() {
    (a as String).length
}

class B {
    fun bar() {}
}
context(b: B)
fun test4() {
    b.bar()
}

context(a: Unit)
fun test5() {
    a.toString()
}

context(a: Nothing)
fun test6() {
    a.toString()
}

context(a: Any)
fun test7() {
    a.hashCode()
}

context(a: Int?)
fun test8() {
    a?.inc()
}

context(a: Object)
fun test9() {
    a.wait()
}

context(a: JavaClass<String>)
fun test10() {
    a.foo()
}

context(a: Nothing?)
fun test11() {
    a.toString()
}

fun usage() {
    with("") {
        test1()
    }
    with({ "" }) {
        test2()
    }
    with("") {
        test3<String>()
    }
    with(B()) {
        test4()
    }
    with(Unit) {
        test5()
    }
    with(throw Exception()) {
        test6()
    }
    with(1) {
        test7()
    }
    with(null) {
        test8()
    }
    with(Object()) {
        test9()
    }
    with(JavaClass<String>()){
        test10()
    }
    with(null) {
        test11()
    }
}
