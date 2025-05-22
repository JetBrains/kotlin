// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// FIXATION_LOGS
// DUMP_CONSTRAINTS: MERMAID, MARKDOWN

inline fun <reified P : Any> parse(text: String): P = TODO()

inline fun <T : Any> ifTrue(condition: Boolean, exec: () -> T?): T? = null

fun decode(src: String): String = src

class Result

fun parse(token: String, flag: Boolean): Result? {
    return ifTrue(flag) {
        try {
            // P shouldn't be inferred to Nothing
            parse(decode(token))
        } catch (e: Exception) {
            null
        }
    }
}
