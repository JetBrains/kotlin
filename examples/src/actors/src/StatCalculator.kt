package org.jetbrains.kotlin.examples.actors

import std.concurrent.*
import org.jetbrains.kotlin.examples.actors.Actor
import java.util.concurrent.Executors
import java.util.LinkedList

class StatCalculator() : Actor(Executors.newSingleThreadExecutor().sure()) {
    val list = LinkedList<Long> ()
    var average = 0.long
    var sum = 0.long
    var cnt = 0.long

    val timer = fixedRateTimer(period=2000.long, daemon=true) {
        this@StatCalculator post "print"
    }

    override fun onMessage(msg: Any) {
        when(msg) {
            is Long -> {
                list.add(msg)
                sum += msg
                if (list.size() > 20)
                    sum -= list.removeFirst()
                average = sum / list.size()
                cnt++
            }
            "print" -> {
                println("Avr. period:[${average}ms]  # updates:[$cnt]")
            }
            else -> {}
        }
    }
}
