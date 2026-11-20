// one.B
// LANGUAGE: +CompanionBlocks
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

@JvmInline
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

// DECLARATIONS_NO_LIGHT_ELEMENTS: B.class[foo]
// LIGHT_ELEMENTS_NO_DECLARATION: B.class[foo-dcK3Deo]
