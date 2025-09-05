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
// FILE: BaseFoo.kt

@MustUseReturnValue
open class BaseFoo {
    @IgnorableReturnValue fun x(): String = ""
    fun ign(): String = ""
}

// FILE: jvm.kt

actual class <!ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT("'expect fun x(): String' defined in 'Foo'; must-use; 'fun x(): String' defined in 'BaseFoo'; ignorable"), ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT("'expect fun ign(): String' defined in 'Foo'; ignorable; 'fun ign(): String' defined in 'BaseFoo'; must-use"), ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT("'expect constructor(): Foo' defined in 'Foo'; must-use; 'constructor(): Foo' defined in 'Foo'; unspecified (implicitly ignorable)")!>Foo<!> : BaseFoo() {
}

fun main() {
    Foo()
    Foo().x()
    Foo().<!RETURN_VALUE_NOT_USED!>ign<!>()
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, secondaryConstructor */
