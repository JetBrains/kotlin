// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtImportAlias
// OPTIONS: usages, constructorUsages
package client

import server.Server as Srv<caret>
import server.Server

class Client(name: String = Server.NAME): Srv() {
    var nextServer: Server? = new Server()
    val name = Server.NAME

    /**
     * [Srv] parameter
     */
    fun foo(s: Srv) {
        val server: Server = s
        println("Server: $server")
    }

    fun getNextServer(): Server? {
        return nextServer
    }

    override fun work() {
        super<Server>.work()
        println("Client")
    }

    companion object: Server() {

    }
}

object ClientObject: Server() {

}

class Servers: Iterator<Server> {

}

fun Iterator<Server>.f(p: Iterator<Server>): Iterator<Server> = this

fun Client.bar(s: Server = Server.NAME) {
    foo(s)
}

fun Client.hasNextServer(): Boolean {
    return getNextServer() != null
}

fun Any.asServer(): Server? {
    when (this) {
        is Server -> println("Server!")
    }
    return if (this is Server) this as Server else this as? Server
}

// DISABLE-ERRORS