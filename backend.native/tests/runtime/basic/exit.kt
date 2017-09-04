import kotlin.system.*

fun main(args: Array<String>) {
    exitProcess(42)
    @Suppress("UNREACHABLE_CODE")
    throw RuntimeException("Exit function call returned normally")
}