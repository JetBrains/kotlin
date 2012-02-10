package org.jetbrains.kotlin.examples.actors

import java.util.concurrent.Executors

fun main(args: Array<String>) {
    val numberOfSymbols  = 10000
    val numberOfShards   = 10
    val numberOfClients  = 100000
    val stocksPerClient  = 4

    val stockServer = StockServer(numberOfShards)

    for (i in 0..numberOfSymbols)
        stockServer post RegisterStock(i.toString())

    println("Stock server started")

    val executor = Executors.newFixedThreadPool(4*Runtime.getRuntime().sure().availableProcessors()).sure()

    for(i in 0..numberOfClients) {
        val client = executor.actor{ message ->
        //            println(message)
        }

        for (k in 1..stocksPerClient) {
            val stock = (Math.random() * numberOfSymbols).int
            stockServer post Subscribe(stock.toString(), client)
        }
    }

    println("clients connected")
}
