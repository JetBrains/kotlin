// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks
// WITH_STDLIB

@JvmInline
value class C1(<!CONFLICTING_JVM_DECLARATIONS!>val value: String<!>) {
    companion {
        val value: String <!CONFLICTING_JVM_DECLARATIONS!>get()<!> = "companion"
    }
}

@JvmInline
value class C2(<!CONFLICTING_JVM_DECLARATIONS!>val value: String<!>) {
    companion {
        <!CONFLICTING_JVM_DECLARATIONS!>val value: String<!> = "companion"
    }
}

@JvmInline
value class C3(<!CONFLICTING_JVM_DECLARATIONS!>val value: C1<!>) {
    companion {
        val value: C1 <!CONFLICTING_JVM_DECLARATIONS!>get()<!> = C1("companion")
    }
}

@JvmInline
value class C4(<!CONFLICTING_JVM_DECLARATIONS!>val value: C1<!>) {
    companion {
        <!CONFLICTING_JVM_DECLARATIONS!>val value: C1<!> = C1("companion")
    }
}

@JvmInline
value class C5(<!CONFLICTING_JVM_DECLARATIONS!>val value: List<Int><!>) {
    companion {
        <!CONFLICTING_JVM_DECLARATIONS!>val value: List<String><!> = listOf()
    }
}

@JvmInline
value class C6(val value: Int) {
    companion {
        val value: String = "abc"
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, primaryConstructor, propertyDeclaration, stringLiteral, value */
