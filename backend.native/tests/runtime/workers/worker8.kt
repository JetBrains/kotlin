package runtime.workers.worker8

import kotlin.test.*

import konan.worker.*

data class SharedDataMember(val double: Double)

data class SharedData(val string: String, val int: Int, val member: SharedDataMember)

@Test fun runTest() {
    val worker = startWorker()
    // Here we do rather strange thing. To test object detach API we detach object graph,
    // pass C pointer as a value to worker, where we manually reattached passed value.
    val future = worker.schedule(TransferMode.CHECKED, {
        detachObjectGraph { SharedData("Hello", 10, SharedDataMember(0.1)) }
    } ) {
        inputC ->
        val input = attachObjectGraph<SharedData>(inputC)
        println(input)
    }
    future.consume {
        result -> println("Got $result")
    }
    worker.requestTermination().consume { _ -> }
    println("OK")
}