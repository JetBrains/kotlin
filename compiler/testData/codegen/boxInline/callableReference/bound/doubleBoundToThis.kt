// SKIP_SOURCEMAP_REMAPPING
// KT-47767

// FILE: 1.kt

public typealias LoggingFunctionType<T> = () -> T

inline fun testLoggingPassThough(loggerMethod: LoggingFunctionType<String>): String {
    return loggerMethod() + loggerMethod() // if this call is commented the issue doesn't reproduce
}

// FILE: 2.kt

class LLoggerTest {
    private var i = 0
    fun testDebugTag(): String {
        return testLoggingPassThough(
            ::forRef
        )
    }
    private fun forRef(): String {
        if (i == 0) {
            i++
            return "O"
        }
        return "K"
    }
}

fun box(): String {

    return LLoggerTest().testDebugTag()
}