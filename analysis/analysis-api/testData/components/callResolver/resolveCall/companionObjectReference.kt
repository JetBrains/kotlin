open class Server() {
    companion object {
        val NAME = "Server"
    }
}

class Client: Server() {
    val name = <expr>Server</expr>.NAME
}