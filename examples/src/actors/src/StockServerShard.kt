package org.jetbrains.kotlin.examples.actors

import std.concurrent.*
import java.util.concurrent.Executor
import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.HashMap
import java.util.HashSet

class StockServerShard(val statCalculator : StatCalculator, executor: Executor) : Actor(executor) {
    private val stocks = HashMap<String,Stock>();

    private var previousTime = System.currentTimeMillis();

    private var updateScheduled = false

    override fun onMessage(message: Any) {
        when(message) {
            StockMessage.DoUpdateModel -> {
                val timeMillis = System.currentTimeMillis()
                for(s in stocks.values()) {
                    s.update()
                }
                statCalculator post (timeMillis - previousTime)
                previousTime = timeMillis
                updateScheduled = false
            }
            StockMessage.UpdateModel -> {
                if (!updateScheduled) {
                    this post StockMessage.DoUpdateModel
                    updateScheduled = true
                }
            }
            is RegisterStock ->
                stocks.put(message.symbol, Stock(message.symbol))
            is Subscribe ->
                stocks.get(message.symbol)?.subscribers?.add(message.subscriber)
            is Unsubscribe ->
                stocks.get(message.symbol)?.subscribers?.add(message.subscriber)
            else -> {}
        }
    }
}

