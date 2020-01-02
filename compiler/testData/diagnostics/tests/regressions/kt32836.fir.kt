// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

inline fun <reified T> parse(json: String): T? = TODO()

class MyType

fun parseMyData(json: String): MyType =
    try {
        parse(json) // error with new inf only: Cannot use 'Nothing?' as reified type parameter
            ?: throw IllegalArgumentException("Can't parse")
    } catch (e: Exception) {
        throw IllegalArgumentException("Can't parse")
    }

fun main() {
    parseMyData("")
}