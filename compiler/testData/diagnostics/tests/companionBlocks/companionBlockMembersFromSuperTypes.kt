// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: A.java
public class A extends K {
    public static void foo() {}
}

// FILE: B.java
public class B {
    public static void baz() {}
}

// FILE: test.kt
open class K : B() {
    companion {
        fun bar() {
            bar()
            baz()
        }
    }

    companion object {
        fun companionObjectK() {
            bar()
            baz()
        }
    }

    fun memberK() {
        bar()
        baz()
    }
}

class KK : K() {
    companion {
        fun qux() {
            bar()
            baz()
            qux()
        }
    }

    companion object {
        fun companionObjectKK() {
            bar()
            baz()
            qux()
        }
    }

    fun memberKK() {
        bar()
        baz()
        qux()
    }
}

fun test() {
    // Java receiver => find statics from Java (transitive) supertypes only
    A.foo()
    A.<!UNRESOLVED_REFERENCE!>bar<!>()
    A.baz()

    // Kotlin receiver => only companion members declared directly in the class
    K.bar()
    K.<!UNRESOLVED_REFERENCE!>baz<!>()

    KK.qux()
    KK.<!UNRESOLVED_REFERENCE!>bar<!>()
    KK.<!UNRESOLVED_REFERENCE!>baz<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaFunction, javaType */
