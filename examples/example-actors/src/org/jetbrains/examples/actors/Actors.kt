package org.jetbrains.examples.actors

import std.concurrent.*
import std.util.*

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.Date

class App() : Actor(Executors.newFixedThreadPool(10).sure()) {
    private val logger = executor.actor { message ->
        println("${Date()}:\t\t$message")
    }

    private val actors = Array<Actor>(100, { createChild(it) });

    private var finishedChildren = actors.size

    override fun onMessage(message: Any) {
        when(message) {
            "start" -> {
                logger post "app started"
                for(a in actors) {
                    a post "start"
                }
            }
            "child finished" -> {
                if(--finishedChildren == 0) {
                    logger send "app finished"
                    (executor as ExecutorService).shutdown()
                }
            }
            else -> {
                logger post "unknown message $message"
            }
        }
    }

    private fun createChild(index: Int) : Actor = actor { message ->
        val next = (index + 1) % actors.size
        when(message) {
            "start" -> {
                logger post "$index started"
                actors[next] post #(index, 0)
            }
            is Tuple2<Int,Int>  -> {
                logger post "$index received $message"
                val from  = message._1
                val value = message._2
                if(next != from) {
                    actors[next] post #(from, value+1)
                }
                else {
                    logger  post "$index finished"
                    this@App post "child finished"
                }
            }
            else -> {}
        }
    }
}

fun main(args: Array<String>) {
    App() post "start"
}