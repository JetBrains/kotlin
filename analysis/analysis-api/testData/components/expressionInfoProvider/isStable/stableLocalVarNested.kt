fun main() {
    var local: C? = null
    local = C()

    run {
        println(<expr>local</expr> != null)
    }
}

class C