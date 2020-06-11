// "Change parameter 'code' type of primary constructor of class 'TestDelegate' to '() -> Logger'" "true"
// WITH_RUNTIME
import kotlin.reflect.KProperty

object Test {
    val logger by TestDelegate {
        <caret>Logger(LoggerConfig("From delegate"))
    }
}


class TestDelegate(val code: () -> LoggerConfig) {
    operator fun getValue(kalGlobal: Test, property: KProperty<*>): Any {
        return code.invoke()
    }
}

data class LoggerConfig(val name: String)
data class Logger(val loggerConfig: LoggerConfig)