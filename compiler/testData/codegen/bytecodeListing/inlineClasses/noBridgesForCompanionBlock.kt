// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +CompanionBlocks

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineString(val value: String)

open class A {
    companion {
        fun foo(s: InlineString) {}
    }
}

class B : A() {
    companion {
        fun foo(s: InlineString) {}
    }
}
