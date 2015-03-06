//no nested class access via instance reference error
fun test() {
    A.Default.f(<!TYPE_MISMATCH!>""<!>)
}

class A() {
    default object {
        object f {
            fun invoke(i: Int) = i
        }
    }
}
