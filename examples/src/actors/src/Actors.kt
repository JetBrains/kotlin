package org.jetbrains.kotlin.examples.actors

import std.concurrent.*
import std.util.*

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicLong

/*
Message handling class which process only one message at any given moment

Three main ways to use it
- post and forget
- post and await result
- post and assign callback to be called when message processed
*/
abstract class Actor(protected val executor: Executor, val fair: Boolean = false) {
    private val queue = Ref()

    class Ref() : AtomicReference<FunctionalQueue<Any>>(emptyQueue), Runnable {
        override fun run() {
            if (!fair)
                runUnfair()
            else
                runFair()
        }

        private fun runFair() {
            while(true) {
                val q = get()
                if(q.identityEquals(busyEmptyQueue)) {
                    if(compareAndSet(busyEmptyQueue, emptyQueue)) {
                        break
                    }
                }
                else {
                    val removed = q.removeFirst()
                    val newQueue = if(removed._2.empty) busyEmptyQueue else removed._2
                    if(compareAndSet(q, newQueue)) {
                        doProcess(removed._1)
                        executor.execute(this)
                        break
                    }
                }
            }
        }

        private fun runUnfair() {
            while (true) {
                val q = get()
                if(q.identityEquals(busyEmptyQueue)) {
                    if (compareAndSet(q, emptyQueue)) {
                        break
                    }
                }
                else {
                    if (compareAndSet(q, busyEmptyQueue)) {
                        var l = q.output
                        while (!l.empty) {
                            doProcess(l.head)
                            l = l.tail
                        }
                        l = q.input.reversed()
                        while (!l.empty) {
                            doProcess(l.head)
                            l = l.tail
                        }
                    }
                }
            }
        }

        override fun toString(): String? = null
    }

    /*
    Handle message and return result
    This method guaranteed to be executed only one per object at any given time
    */
    protected abstract fun onMessage(message: Any) : Any?

    /*
    Post message to the actor.
    The method returns immediately and the message itself will be processed later
    */
    fun post(message: Any) {
        messagesSent.incrementAndGet()
        while(true) {
            val q = queue.get()
            val newQueue = (if (q.identityEquals(busyEmptyQueue)) emptyQueue else q) add message
            if(queue.compareAndSet(q, newQueue)) {
                if (q.identityEquals(emptyQueue)) {
                    executor.execute(queue)
                }
                break
            }
        }
    }

    fun postFirst(message: Any) {
        messagesSent.incrementAndGet()
        while(true) {
            val q = queue.get()
            val newQueue = (if (q.identityEquals(busyEmptyQueue)) emptyQueue else q) addFirst message
            if(queue.compareAndSet(q, newQueue)) {
                if (q.identityEquals(emptyQueue)) {
                    executor.execute(queue)
                }
                break
            }
        }
    }

    /*
    Post message to the actor and schedule callback to be executed on given executor when message processed
    */
    fun post(message: Any, executor: Executor = this.executor, callback: (Any?)->Unit) =
        post(Callback(message, executor, callback))

    /*
    Send message to the actor and await result
    */
    fun send(message: Any) : Any? {
        val request = Request(message)
        post(request)
        request.await()
        return request.result
    }

    private fun doProcess(message: Any) {
        messagesProcessed.incrementAndGet()
        when(message) {
            is Request -> {
                message.result = onMessage(message.message)
                message.countDown()
            }
            is Callback -> {
                val result = onMessage(message.message)
                message.executor execute {
                    val callback = message.callback;
                    callback(result)
                }
            }
            else -> onMessage(message)
        }
    }

    /*
    Create actor on the same executor
    */
    fun actor(handler: (Any)->Any?) = executor.actor(handler)

    class object {
        val emptyQueue = FunctionalQueue<Any>()
        val busyEmptyQueue = FunctionalQueue<Any>() add "busy empty queue"

        class Request(val message: Any) : java.util.concurrent.CountDownLatch(1) {
            var result: Any? = null
        }

        class Callback(val message: Any, val executor: Executor, val callback: (Any?) -> Unit)

        val messagesSent = AtomicLong()
        val messagesProcessed = AtomicLong()

        val timer = fixedRateTimer(daemon=true, period=5000.toLong()) {
            val sent = messagesSent.get()
            val received = messagesProcessed.get()
            println("Actors stat: Sent: $sent Processed: $received Pending: ${sent-received}")
        }
    }
}

fun Executor.actor(handler: (Any)->Any?) : Actor = object: Actor(this) {
    override fun onMessage(message: Any) {
        handler(message)
    }
}

fun singleThreadActor(handler: (Any)->Any?) : Actor = Executors.newSingleThreadExecutor().sure().actor(handler)
