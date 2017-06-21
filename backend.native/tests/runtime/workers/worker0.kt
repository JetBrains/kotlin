import konan.worker.*

fun main(args: Array<String>) {
    val worker = startWorker()
    val future = worker.schedule(TransferMode.CHECKED, { "Input".shallowCopy()}) {
        input -> input + " processed"
    }
    future.consume {
        result -> println("Got $result")
    }
    worker.requestTermination().consume { _ -> }
    println("OK")
}