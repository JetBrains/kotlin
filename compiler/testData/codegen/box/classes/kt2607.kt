fun box() : String {
    val o = object {

        inner class C {
            fun foo() = "OK"
        }
    }
    return o.C().foo()
}
