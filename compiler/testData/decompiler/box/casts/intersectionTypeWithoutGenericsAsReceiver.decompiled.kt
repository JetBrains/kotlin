interface A {
}
interface B {
}
class C: A, B {
}
fun <T  : A, B> T.foo() : String  {
    return "OK"
}

fun box() : String  {
    return C().foo<C>()
}
