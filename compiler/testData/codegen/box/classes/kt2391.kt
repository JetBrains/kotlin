// KJS_WITH_FULL_RUNTIME
// JVM_ABI_K1_K2_DIFF: KT-63864

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
