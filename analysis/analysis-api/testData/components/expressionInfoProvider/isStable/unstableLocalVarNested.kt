fun main() {
    var local: C? = C()

    run {
        println(<expr>local</expr> != null)
    }

    local = null
}

class C {
    val c: C? = null
}