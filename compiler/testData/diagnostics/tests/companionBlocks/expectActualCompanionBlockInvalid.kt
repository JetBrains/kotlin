// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    companion {
        fun foo()
        val x: String
        var y: Int
    }
}

expect class Foo2 {
    fun foo()
    val x: String
    var y: Int
}

expect class Bar {
    companion {
        fun foo()
    }
}

// MODULE: m2-jvm()()(m1-common)
// FILE: J.java
public class J {
    public void foo() {}
}

// FILE: jvm.kt
actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> {
    actual fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
    actual val <!ACTUAL_WITHOUT_EXPECT!>x<!>: String get() = ""
    actual var <!ACTUAL_WITHOUT_EXPECT!>y<!>: Int = 0
}

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo2<!> {
    companion {
        actual fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
        actual val <!ACTUAL_WITHOUT_EXPECT!>x<!>: String get() = ""
        actual var <!ACTUAL_WITHOUT_EXPECT!>y<!>: Int = 0
    }
}

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Bar<!> = J

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, getter, integerLiteral,
propertyDeclaration, stringLiteral */
