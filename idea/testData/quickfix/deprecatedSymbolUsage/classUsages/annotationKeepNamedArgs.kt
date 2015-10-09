// "Replace with 'Bar(p, "")'" "true"

package test

@Deprecated("Replace with bar", ReplaceWith("Bar(p, \"\")"))
annotation class Foo(val p: Int)

annotation class Bar(val p: Int, val s: String)

@Foo<caret>(p = 1) class C