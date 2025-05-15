// RUN_PIPELINE_TILL: FRONTEND
// FILE: main.kt
interface A {
    suspend fun foo()
    fun bar()
}

interface B : A {
    override fun <!SUSPEND_OVERRIDDEN_BY_NON_SUSPEND!>foo<!>() {

    }

    override suspend fun <!NON_SUSPEND_OVERRIDDEN_BY_SUSPEND!>bar<!>() {

    }
}

interface C : A {
    suspend override fun foo() {

    }

    override fun bar() {

    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class D<!> : J {
    suspend override fun foo() {

    }
}

// FILE: J.java

public interface J extends A {
    Object foo(kotlin.coroutines.Continuation<kotlin.Unit> y);
}
