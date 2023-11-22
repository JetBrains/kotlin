import kotlin.native.concurrent.*

fun foo(x: Int) {
    val worker = Worker.start()
    worker.execute(TransferMode.SAFE, { "zzz" }) { s -> s + x.toString() }
}
