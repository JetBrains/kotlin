// JDK_RELEASE: 16
import kotlin.concurrent.thread

fun box(): String {
    val myThread: Thread = thread(start = false) {  }
    return if (myThread.state == Thread.State.NEW) "OK" else "fail"
}
