namespace threadring

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

val  MAX_NODES   = 503
val  MAX_THREADS = 503

val cdl = CountDownLatch(1)

fun main(args: Array<String>) {
    val start = System.currentTimeMillis()
    var n = 5000000;
    try {
        n = Integer.parseInt(args[0]);
    } catch (e: Throwable) {
    }

    val ring = ThreadRing(n)
    ring.sendMessage(TokenMessage(1,0,false))
    cdl.await();

    val total = System.currentTimeMillis() - start
    System.out?.println("[ThreadRing-" + System.getProperty("project.name")+ " Benchmark Result: " + total + "]")
}

class TokenMessage(val nodeId : Int, value: Int, val isStop: Boolean) : AtomicInteger(value){
}

class ThreadRing(val N: Int) {
    val executor = Executors.newFixedThreadPool(MAX_THREADS).sure()

    val nodes : Array<Node> = Array<Node>(MAX_NODES+1, { Node(it+1) })

    {
        connectNodes()
    }

    fun connectNodes() {
        nodes[nodes.size-1] = nodes[0]
        for (i in 0..nodes.size-2) {
            nodes[i].connect(nodes[i+1]);
        }
    }

    fun sendMessage(m : TokenMessage) {
        nodes[0].sendMessage(m)
    }
    class Node(val nodeId : Int) : Runnable {
        val queue = LinkedBlockingQueue<TokenMessage>()
        var isActive = false
        var nextNode : Node? = null

        fun sendMessage(m: TokenMessage) {
            queue.add(m)
            executor.execute(this)
        }

        fun connect(next: Node) {
            nextNode = next
            isActive = true
        }

        override fun run() {
            if(isActive) {
                try {
                    val m = queue.take()
                    if(m.isStop) {
                        val nextValue = m.get()+1
                        if (nextValue == MAX_NODES) {
                            executor.shutdown()
                            cdl.countDown()
                        } else {
                            m.set(nextValue)
                            nextNode.sure().sendMessage(m)
                        }
                        isActive = false
                    }
                    else {
                        if (m.get() == N) {
                            System.out?.println(nodeId);
                            nextNode.sure().sendMessage(TokenMessage(nodeId, 0, true));
                        } else {
                            m.incrementAndGet()
                            nextNode.sure().sendMessage(m);
                        }
                    }
                }
                catch(e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
