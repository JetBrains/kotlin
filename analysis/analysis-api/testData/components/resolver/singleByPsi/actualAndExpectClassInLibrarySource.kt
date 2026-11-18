// MODULE: lib
// MODULE_KIND: LibrarySource
// LANGUAGE: +MultiPlatformProjects
// FILE: jvm.kt
@file:Suppress("EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE", "ACTUAL_WITHOUT_EXPECT")

package test

actual class Foo

fun usage(foo: Foo) = foo.hash<caret>Code()

// FILE: common.kt
@file:Suppress("EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE")

package test

expect class Foo {
    override fun hashCode(): Int
}
