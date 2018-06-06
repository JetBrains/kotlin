package runtime.workers.worker3

import kotlin.test.*

import konan.worker.*

data class DataParam(var int: Int)
data class WorkerArgument(val intParam: Int, val dataParam: DataParam)
data class WorkerResult(val intResult: Int, val stringResult: String)

@Test fun runTest() {
    main(emptyArray())
}

fun main(args: Array<String>) {
    val worker = startWorker()
    val dataParam = DataParam(17)
    val future = try {
        worker.schedule(TransferMode.CHECKED,
                { WorkerArgument(42, dataParam) },
                { input -> WorkerResult(input.intParam, input.dataParam.toString() + " result") }
        )
    } catch (e: IllegalStateException) {
        null
    }
    if (future != null)
        println("Fail 1")
    if (dataParam.int != 17) println("Fail 2")
    worker.requestTermination().consume { _ -> }
    println("OK")
}