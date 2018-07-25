// IGNORE_BACKEND: JVM_IR
fun box() : String {
    val o = object {

        inner class C {
            fun foo() = "OK"
        }
    }
    return o.C().foo()
}
