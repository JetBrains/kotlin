// MODULE: lib
// MODULE_KIND: LibrarySource
// LANGUAGE: +MultiPlatformProjects
// FILE: common.kt
@file:Suppress("EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE")

package test

expect class Foo {
    override fun hashCode(): Int
}

fun usage(foo: F<caret_type>oo) = foo.hash<caret>Code()

// FILE: jvm.kt
@file:Suppress("EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE", "ACTUAL_WITHOUT_EXPECT")

package test

actual class Foo
