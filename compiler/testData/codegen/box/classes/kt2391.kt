// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_STRING_BUILDER
// KJS_WITH_FULL_RUNTIME
public interface LoggerAware {
    public val logger: StringBuilder
}

public abstract class HttpServer(): LoggerAware {
    public fun start() {
        logger.append("OK")
    }
}

public class MyHttpServer(): HttpServer() {
    public override val logger = StringBuilder()
}

fun box(): String {
    val server = MyHttpServer()
    server.start()
    return server.logger.toString()!!
}
