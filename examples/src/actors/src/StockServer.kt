package org.jetbrains.kotlin.examples.actors

import std.concurrent.*
import java.util.concurrent.Executor
import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.HashMap
import java.util.HashSet

class StockServer(val numberOfShards : Int) : Actor(Executors.newFixedThreadPool(numberOfShards+1).sure()) {
    private val stockToServer = HashMap<String,StockServerShard> ()

    private val statCalculator = StatCalculator()

    private val shards = Array<StockServerShard> (numberOfShards, { (i: Int) -> StockServerShard(statCalculator, executor) })

    private val timer = fixedRateTimer(period=1000.toLong(), daemon=true) {
        for(s in shards)
            s post StockMessage.UpdateModel
    }

    override fun onMessage(message: Any): Unit {
        when (message) {
            is RegisterStock -> {
                val shard = shards[stockToServer.size() % shards.size]
                stockToServer.put(message.symbol, shard)
                shard post message
            }
            is Subscribe -> {
                stockToServer.get(message.symbol)?.post(message)
            }
            is Unsubscribe -> {
                stockToServer.get(message.symbol)?.post(message)
            }
            else -> {}
        }
    }
}