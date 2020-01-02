private class X

private operator fun X?.plus(p: Int) = X()

class C {
    private val map = hashMapOf<String, X>()

    fun f() {
        map[""] += 1
    }
}