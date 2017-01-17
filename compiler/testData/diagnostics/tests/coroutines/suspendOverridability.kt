// FILE: main.kt
interface A {
    suspend fun foo()
    fun bar()
}

interface B : A {
    <!CONFLICTING_OVERLOADS!><!NOTHING_TO_OVERRIDE!>override<!> fun foo()<!> {

    }

    <!CONFLICTING_OVERLOADS!><!NOTHING_TO_OVERRIDE!>override<!> suspend fun bar()<!> {

    }
}

interface C : A {
    suspend override fun foo() {

    }

    override fun bar() {

    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class D<!> : J {
    <!ACCIDENTAL_OVERRIDE!>suspend override fun foo()<!> {

    }
}

// FILE: J.java

public interface J extends A {
    Object foo(kotlin.coroutines.Continuation<kotlin.Unit> y);
}
