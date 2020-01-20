// !WITH_NEW_INFERENCE

class B

class A {
    operator fun B.invoke() {}
}

val B.a: () -> Int  get() = { 5 }

fun test(a: A, b: B) {
    val <!UNUSED_VARIABLE!>x<!>: Int = b.a()

    b.<!FUNCTION_EXPECTED!>(a)<!>()

    <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>(b) {
        val <!UNUSED_VARIABLE!>y<!>: Int = a()
        <!FUNCTION_EXPECTED!>(a)<!>()
    }
}
