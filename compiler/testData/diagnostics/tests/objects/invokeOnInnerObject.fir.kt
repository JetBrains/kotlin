//no nested class access via instance reference error
fun test() {
    A.Companion.<!INAPPLICABLE_CANDIDATE!>f<!>("")
}

class A() {
    companion object {
        object f {
            operator fun invoke(i: Int) = i
        }
    }
}
