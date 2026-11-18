// MODULE: lib
// MODULE_KIND: LibrarySource
// LANGUAGE: +MultiPlatformProjects
// FILE: common.kt
@file:Suppress("EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE")

package test

expect class Foo {
    class Nested {
        override fun hashCode(): Int
    }
}

// FILE: jvm.kt
@file:Suppress("EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE", "ACTUAL_WITHOUT_EXPECT")

package test

actual class Foo {
    actual class Nested
}

fun usage(nested: Foo.Nested) = nested.hash<caret>Code()
