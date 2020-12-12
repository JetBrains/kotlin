// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
// !LANGUAGE: +NewInference

interface JsonParser
interface JsonCodingParser : JsonParser

var result = "fail"

fun JsonCodingParser.parseValue(source: String): Any = source
fun JsonParser.parseValue(source: String): Any = TODO()

fun testDecoding(decode: (String) -> Any) {
    result = decode("OK") as String
}

class Test {
    fun fooTest() {
        val foo: JsonCodingParser = object : JsonCodingParser {}
        testDecoding(foo::parseValue)
    }
}

fun box(): String {
    Test().fooTest()
    return result
}