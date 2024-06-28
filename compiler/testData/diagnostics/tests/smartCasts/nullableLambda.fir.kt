// ISSUE: KT-4113
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
class Test1(val lambda: (() -> String)?) {
    fun foo() {
        if (lambda != null) {
            lambda.invoke()
            lambda()
        }
    }
}

fun test2(lambda: (() -> String)?) {
    if (lambda != null) {
        lambda.invoke()
        lambda()
    }
}

class A
operator fun A.invoke(): Unit = TODO()

class Test3 {
    val nullableCallableClass: A? = null
    fun foo() {
        if (nullableCallableClass != null) {
            nullableCallableClass()
        }
    }
}

fun test4(nullableCallableClass: A?){
    if (nullableCallableClass != null) {
        nullableCallableClass()
    }
}

class B {
    operator fun invoke(s: String): (() -> String)? = TODO()
    operator fun invoke(): (() -> String) = TODO()
}

class Test4 {
    fun foo(a: B) {
        if (a("") != null) {
            a()()
            <!UNSAFE_IMPLICIT_INVOKE_CALL!>a("")<!>()
        }
    }
}

fun test5(a: B) {
    if (a("") != null) {
        a()()
        <!UNSAFE_IMPLICIT_INVOKE_CALL!>a("")<!>()
    }
}