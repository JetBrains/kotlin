abstract class C {
    fun test(x: Int) {
        if (x == 0) return
        if (this is D) {
            (this: D).test(x - 1)
        }
    }
}

class D: C()

fun box(): String {
    D().test(10)
    return "OK"
}
