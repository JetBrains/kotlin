// "Replace with 'test.Bar'" "true"

package test

@deprecated("Replace with bar", ReplaceWith("test.Bar"))
annotation class Foo

annotation class Bar

@Foo<caret> class C {}