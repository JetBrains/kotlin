//no nested class access via instance reference error
fun test() {
    A.f(<!TYPE_MISMATCH!>""<!>)
}

class A() {
    class object {
        object f {
            fun invoke(i: Int) = i
        }
    }
}
