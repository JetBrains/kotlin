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



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_STRING_BUILDER
