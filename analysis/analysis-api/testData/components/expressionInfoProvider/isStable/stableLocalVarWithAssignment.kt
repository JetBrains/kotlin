fun main() {
    var local: C? = C()

    println(<expr>local</expr> != null)

    run {
        local = null
    }
}

class C {
    val c: C? = null
}