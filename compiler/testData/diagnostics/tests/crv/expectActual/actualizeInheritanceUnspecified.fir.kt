// LANGUAGE: +MultiPlatformProjects
// RUN_PIPELINE_TILL: BACKEND

// MODULE: m1-common
// FILE: common.kt

@file:MustUseReturnValue

expect class Foo() {
    fun x(): String
    @IgnorableReturnValue fun ign(): String
}

fun commonMain() {
    <!RETURN_VALUE_NOT_USED!>Foo<!>()
    Foo().<!RETURN_VALUE_NOT_USED!>x<!>()
    Foo().ign()
}


// MODULE: m2-jvm()()(m1-common)
// FILE: JavaFoo.java

public class JavaFoo {
    public String x() {
        return "";
    }
    public String ign() {
        return "";
    }
}

// FILE: jvm.kt

// Foo.<init> and Java methods are Unspecified.
// We report mismatch to Unspecified only if there is a meaningful (i.e. member) declaration to report on.
actual class <!ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT("'expect constructor(): Foo' defined in 'Foo'; must-use; 'constructor(): Foo' defined in 'Foo'; unspecified (implicitly ignorable)")!>Foo<!> : JavaFoo() {
}

fun main() {
    Foo()
    Foo().x()
    Foo().ign()
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, secondaryConstructor */
