// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81262
// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
// FILE: Super.java
class Super {
    public static void foo() {}
}

// FILE: Sub.java
public class Sub extends Super {
}

// FILE: test.kt
interface I

fun I.extensionFoo() {
}

class C {
    protected companion object : I {
        fun foo() {
        }
    }

    protected object O : I

    internal <!NOTHING_TO_INLINE!>inline<!> fun bar() {
        C.foo() // should be yellow
        foo() // should be yellow
        extensionFoo() // should be yellow
        with(O) {
            extensionFoo()
        }
    }
}

// FILE: test2.kt
import O.foo
import Super.foo as fooSuper
import Sub.foo as fooSub

private object O {
    fun foo() {
    }
}

internal <!NOTHING_TO_INLINE!>inline<!> fun inlineFun() {
    O
    O.<!PRIVATE_CLASS_MEMBER_FROM_INLINE!>foo<!>() // should be yellow
    <!PRIVATE_CLASS_MEMBER_FROM_INLINE!>foo<!>() // should be yellow
    Super.foo() // should be yellow
    Sub.foo() // should be green
    fooSuper() // should be yellow
    fooSub() // should be green
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline */
