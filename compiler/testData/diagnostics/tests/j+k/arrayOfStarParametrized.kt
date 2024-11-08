// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// FULL_JDK
import java.util.concurrent.Executors

fun main() {
    val executorService = Executors.newWorkStealingPool()
    val future = executorService.submit { "test" }.get().length
}
