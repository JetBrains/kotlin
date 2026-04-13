// TARGET_BACKEND: JVM
// FULL_JDK
// IGNORE_BACKEND: ANDROID
// LANGUAGE: +EagerLambdaAnalysis
// ISSUE: KT-7052

import java.util.concurrent.Executors

fun box(): String {
    val executorService = Executors.newWorkStealingPool()
    val future = executorService.submit { "OK" }
    executorService.shutdown()
    return future.get()
}
