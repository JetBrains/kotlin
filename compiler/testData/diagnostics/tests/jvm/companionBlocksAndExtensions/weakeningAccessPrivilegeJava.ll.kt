// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks +JvmProhibitWeakeningAccessPrivilegeForStatics
// ISSUE: KT-90567

// FILE: J.java
public class J {
    public static void publicFoo() {}
    static void packagePrivateFoo() {}
    protected static void protectedFoo() {}
    private static void privateFoo() {}
}

// FILE: J2.java
public class J2 extends J {}

// FILE: otherpackage/J3.java
package otherpackage;
public class J3 {
    public static void publicFoo() {}
    static void packagePrivateFoo() {}
    protected static void protectedFoo() {}
    private static void privateFoo() {}
}

// FILE: main.kt
import otherpackage.J3

class A : J() {
    companion {
        fun publicFoo() {}
        fun packagePrivateFoo() {}
        fun protectedFoo() {}
        fun privateFoo() {}
    }
}

class B : J() {
    companion {
        private fun publicFoo() {}
        private fun packagePrivateFoo() {}
        private fun protectedFoo() {}
        private fun privateFoo() {}
    }
}

class C : J() {
    companion {
        protected fun publicFoo() {}
        protected fun packagePrivateFoo() {}
        protected fun protectedFoo() {}
        protected fun privateFoo() {}
    }
}

class D : J() {
    companion {
         internal fun publicFoo() {}
         internal fun packagePrivateFoo() {}
         internal fun protectedFoo() {}
         internal fun privateFoo() {}
    }
}

class E : J2() {
    companion {
        private fun publicFoo() {}
        private fun packagePrivateFoo() {}
        private fun protectedFoo() {}
        private fun privateFoo() {}
    }
}

class F : J3() {
    companion {
        private fun publicFoo() {}
        private fun packagePrivateFoo() {}
        private fun protectedFoo() {}
        private fun privateFoo() {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType */
