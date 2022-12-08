annotation class A(vararg val strings: String)

@A(strings = ["foo", "bar"])
class F<caret>oo
