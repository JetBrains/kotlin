// LANGUAGE: +MultiPlatformProjects
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: m1-common
// RETURN_VALUE_CHECKER_MODE: CHECKER
// FILE: common.kt

expect interface Foo {
    fun close()
}


// MODULE: m2-jvm()()(m1-common)
// RETURN_VALUE_CHECKER_MODE: CHECKER
// FILE: Readable.java

public interface Readable {
    public String read();
}


// FILE: jvm.kt

@MustUseReturnValues
actual interface Foo : Readable {
    actual fun <!ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT!>close<!>()
    override fun read(): String
}

fun main(f: Foo) {
    f.<!RETURN_VALUE_NOT_USED!>read<!>()
    f.close()
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, secondaryConstructor */
