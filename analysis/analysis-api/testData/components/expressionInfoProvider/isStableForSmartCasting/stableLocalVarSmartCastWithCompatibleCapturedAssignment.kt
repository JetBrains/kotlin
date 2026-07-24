fun main() {
    var local: String? = ""
    fun update() {
        local = "value"
    }

    if (local != null) {
        update()
        println(<expr>local</expr>.length)
    }
}
