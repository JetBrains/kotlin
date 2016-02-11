// Enable when JS backend supports local classes
// TARGET_BACKEND: JVM
fun box() : String {
    val o = object {

        inner class C {
            fun foo() = "OK"
        }
    }
    return o.C().foo()
}
