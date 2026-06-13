fun main() {
    var local: C? = C()
    run {
        local = C()
    }
    println(<expr>local</expr> != null)
}

class C {
    val c: C? = null
}
