annotation class A(vararg val strings: String)

@A(*arrayOf("foo", "bar"), "baz", *["quux"])
class F<caret>oo