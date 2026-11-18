// MODULE: lib
// MODULE_KIND: LibrarySource
// LANGUAGE: +MultiPlatformProjects
// FILE: common.kt
@file:Suppress("EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE")

package test

expect class Foo {
    override fun hashCode(): Int
}

fun usage(foo: Foo) {
    <caret_context>Unit
}

// FILE: jvm.kt
@file:Suppress("EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE", "ACTUAL_WITHOUT_EXPECT")

package test

actual class Foo

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: lib

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
(foo as Foo).hash<caret>Code()
