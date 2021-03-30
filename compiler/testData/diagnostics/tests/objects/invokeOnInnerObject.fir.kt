//no nested class access via instance reference error
fun test() {
    A.Companion.f(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
}

class A() {
    companion object {
        object f {
            operator fun invoke(i: Int) = i
        }
    }
}
