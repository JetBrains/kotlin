class C(val f : Int)

fun test(e : Any) {
    if (e is C) {
        #(e.f)
    }
}
