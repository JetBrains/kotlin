// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks +JvmProhibitWeakeningAccessPrivilegeForStatics
// ISSUE: KT-90567

// FILE: A.kt
open class A {
    companion {
        fun bar() {}
    }
}

// FILE: B.java
public class B extends A {}

// FILE: C.kt
class C : B() {
    companion {
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>private<!> fun bar() {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType */
