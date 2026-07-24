fun main() {
    var local: String? = null

    fun update() {
        local = "value"
    }

    local = ""
    println(<expr>local</expr>.length)
}
