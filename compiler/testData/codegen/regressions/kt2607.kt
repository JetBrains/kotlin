fun box() : String {
    val o = object {

        class C {
            fun foo() = "OK"
        }
    }
    return o.C().foo()
}