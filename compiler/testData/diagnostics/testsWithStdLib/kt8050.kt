private class X

private operator fun X?.plus(<!UNUSED_PARAMETER!>p<!>: Int) = X()

class C {
    private val map = hashMapOf<String, X>()

    fun f() {
        map<!NO_SET_METHOD!>[""]<!> += 1
    }
}