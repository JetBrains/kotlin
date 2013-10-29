package client

import server.Server

class Client(): Server() {
    override fun work() {
        super<Server>.work()
        println("Client")
    }
}