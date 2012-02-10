package org.jetbrains.kotlin.examples.actors

import std.concurrent.*
import java.util.concurrent.Executor
import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.HashMap
import java.util.HashSet

open class StockMessage(val symbol: String) {
    class object {
        val UpdateModel = "update"
        val DoUpdateModel = "do update"
    }
}

class RegisterStock(symbol: String)  : StockMessage(symbol)

class Subscribe(symbol: String, val subscriber: Actor)  : StockMessage(symbol)

class Unsubscribe(symbol: String, val subscriber: Actor)  : StockMessage(symbol)

class PriceUpdate(symbol: String, val price: Double) : StockMessage(symbol)
