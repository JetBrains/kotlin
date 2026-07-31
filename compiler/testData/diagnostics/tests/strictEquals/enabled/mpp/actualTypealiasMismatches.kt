// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: commonFile.kt
package p

expect class A {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean
}

expect class J {
    override fun equals(@EqualityBound(J::class) other: Any?): Boolean
}

// MODULE: platform()()(common)
// FILE: q/JavaType.java
package q;

public class JavaType {
}

// FILE: p/platformFile.kt
package p

class B {
    override fun equals(@EqualityBound(Any::class) other: Any?): Boolean = true
}

actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE!>A<!> = B

actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE!>J<!> = q.JavaType

/* GENERATED_FIR_TAGS: actual, classDeclaration, classReference, expect, functionDeclaration, javaType, nullableType,
operator, override, typeAliasDeclaration */
