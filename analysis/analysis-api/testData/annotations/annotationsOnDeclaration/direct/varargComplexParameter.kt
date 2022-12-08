annotation class A(vararg val strings: String)
annotation class AArray(vararg val value: A)

@AArray(A(strings = ["foo", "bar"]))
class F<caret>oo