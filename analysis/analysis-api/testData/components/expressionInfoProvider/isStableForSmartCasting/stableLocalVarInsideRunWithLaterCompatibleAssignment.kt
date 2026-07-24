fun main() {
    var local: C? = C()
    run {
        println(<expr>local</expr> != null)
    }
    local = C()
}

class C {
    val c: C? = null
}
