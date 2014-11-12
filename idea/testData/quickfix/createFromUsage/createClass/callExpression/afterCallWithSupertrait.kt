// "Create class 'Foo'" "true"

trait A

fun test(): A = Foo(2, "2")

class Foo(i: Int, s: String) : A {

}
