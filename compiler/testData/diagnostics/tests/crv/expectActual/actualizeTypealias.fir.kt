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
    <!RETURN_VALUE_NOT_USED!>Foo()<!>
    <!RETURN_VALUE_NOT_USED!>Foo().x()<!>
    Foo().ign()
}


// MODULE: m2-jvm()()(m1-common)
// FILE: BaseFoo.kt

@MustUseReturnValue
open class BaseFoo {
    @IgnorableReturnValue fun x(): String = ""
    fun ign(): String = "42"
}

// FILE: jvm.kt

actual typealias <!ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT("'expect fun x(): String' defined in 'Foo'; must-use; 'fun x(): String' defined in 'BaseFoo'; ignorable"), ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT("'expect fun ign(): String' defined in 'Foo'; ignorable; 'fun ign(): String' defined in 'BaseFoo'; must-use")!>Foo<!> = BaseFoo

fun main() {
    <!RETURN_VALUE_NOT_USED!>Foo()<!>
    Foo().x()
    <!RETURN_VALUE_NOT_USED!>Foo().ign()<!>
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, secondaryConstructor */
