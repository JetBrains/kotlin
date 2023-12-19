// FIR_IDENTICAL
// FULL_JDK
// JVM_TARGET: 1.8

import java.util.concurrent.*

abstract class Logger(val service: ScheduledExecutorService) {
    lateinit var future: ScheduledFuture<CompletableFuture<Void>>

    abstract fun flush(): CompletableFuture<Void>

    fun init() {
        future = service.schedule(this::flush, 0L, TimeUnit.MILLISECONDS)
    }
}
