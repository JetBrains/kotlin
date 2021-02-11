// KT-16549
// IGNORE_BACKEND: JVM

class TailInline {
    private inline fun act(action: () -> Unit) {
        return action()
    }

    private var countDown = 10

    tailrec fun test(): Int {
        if (countDown < 5) return countDown
        act {
            countDown--
            if (countDown < 1)
                return countDown
            else
                return test()  // GOTO countDown--
        }
        return countDown
    }
}

fun box(): String {
    val result = TailInline().test()
    return if (result == 4) "OK" else "Fail: $result"
}
