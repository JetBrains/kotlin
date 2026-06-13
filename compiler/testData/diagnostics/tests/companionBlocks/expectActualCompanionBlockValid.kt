// RUN_PIPELINE_TILL: BACKEND
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

expect class Bar {
    companion {
        fun foo()
    }
}

// MODULE: m2-jvm()()(m1-common)
// FILE: J.java
public class J {
    public static void foo() {}
}

// FILE: jvm.kt
actual class Foo {
    companion {
        actual fun foo() {}
        actual val x: String get() = ""
        actual var y: Int = 0
    }
}

actual typealias Bar = J

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, getter, integerLiteral,
propertyDeclaration, stringLiteral */
