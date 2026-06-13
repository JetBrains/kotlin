fun main() {
    var local: String? = ""
    fun update() {
        local = null
    }

    if (local != null) {
        update()
        println(<expr>local</expr>.length)
    }
}
