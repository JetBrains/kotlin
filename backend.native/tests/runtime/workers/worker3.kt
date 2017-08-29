import konan.worker.*

data class WorkerArgument(val intParam: Int, val stringParam: String)
data class WorkerResult(val intResult: Int, val stringResult: String)

fun main(args: Array<String>) {
    val worker = startWorker()
    val s = "zzz${args.size.toString()}"

    val future = try {
        worker.schedule(TransferMode.CHECKED,
                { WorkerArgument(42, s) },
                { input -> WorkerResult(input.intParam, input.stringParam + " result") }
        )
    } catch (e: IllegalStateException) {
        null
    }
    if (future != null)
        println("Fail 1")
    if (s != "zzz0") println("Fail 2")
    worker.requestTermination().consume { _ -> }
    println("OK")
}