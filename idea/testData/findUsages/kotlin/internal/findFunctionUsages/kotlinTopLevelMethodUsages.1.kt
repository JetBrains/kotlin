package client

import server.processRequest

class Client {
    val methodRef = ::processRequest()

    fun doProcessRequest() {
        println("Process...")
        processRequest()
    }
}