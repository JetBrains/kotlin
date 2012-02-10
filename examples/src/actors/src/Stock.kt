package org.jetbrains.kotlin.examples.actors

import std.concurrent.*
import java.util.concurrent.Executor
import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.HashMap
import java.util.HashSet

class Stock(val symbol: String) {
    val subscribers = HashSet<Actor>()

    var price = 10*Math.random()

    fun update() {
        price += Math.random() - 0.5
        val priceUpdate = PriceUpdate(symbol, price)
        for(s in subscribers) {
            s post priceUpdate
        }
    }
}
