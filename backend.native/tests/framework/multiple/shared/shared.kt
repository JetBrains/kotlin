import kotlin.native.concurrent.Worker

object RuntimeState {
    fun produceChange() {
        Worker.current.executeAfter {}
    }

    fun consumeChange(): Boolean {
        return Worker.current.processQueue()
    }
}