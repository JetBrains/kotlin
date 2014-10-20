// "Create function 'foo' from usage" "true"
class F {
    fun bar() {

    }
}

class X {
    val f: Int = F().<caret>foo(1, "2")
}