// FIR_IDENTICAL
import kotlin.native.concurrent.*

@OptIn(ObsoleteWorkersApi::class)
fun foo(x: Int) {
    val worker = Worker.start()
    worker.execute(TransferMode.SAFE, { "zzz" }) { s -> s + x.toString() }
}
