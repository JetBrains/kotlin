// TARGET_BACKEND: WASM
// WITH_STDLIB
import kotlin.test.*

fun jsRepresentation(x: JsAny): String = js("(typeof x) + ':' + String(x)")

fun box(): String {
    // JsNumber
    val jsNum10: JsNumber = 10.toJsNumber()
    val anotherJsNum10: JsNumber = 10.toDouble().toJsNumber()
    assertTrue(jsNum10 == anotherJsNum10)
    assertTrue(jsNum10.toDouble() == 10.0)
    assertTrue(anotherJsNum10.toInt() == 10)
    val jsNumAsJsAny: JsAny = jsNum10
    assertTrue(jsNumAsJsAny == anotherJsNum10)
    assertTrue(jsNumAsJsAny is JsNumber)
    assertTrue(jsNumAsJsAny !is JsString)
    assertTrue(jsNumAsJsAny !is JsBoolean)
    assertTrue((jsNumAsJsAny as JsNumber).toInt() == 10)
    val jsNumAsAny: Any = jsNum10
    assertTrue(jsNumAsAny == anotherJsNum10)
    assertTrue(jsNumAsAny is JsNumber)
    assertTrue(jsNumAsAny !is JsString)
    assertTrue(jsNumAsAny !is JsBoolean)
    assertTrue(jsNumAsAny !is JsBigInt)
    assertTrue((jsNumAsAny as JsNumber).toInt() == 10)
    assertTrue(jsRepresentation(jsNum10) == "number:10")

    // JsString
    val jsStr1: JsString = "str1".toJsString()
    assertTrue(jsStr1 == "str1".toJsString())
    assertTrue(jsStr1 != "str2".toJsString())
    assertTrue(jsStr1.toString() == "str1")
    assertTrue((jsStr1 as Any) is JsString)
    assertTrue((jsStr1 as Any) !is JsNumber)
    assertTrue((jsStr1 as Any) !is JsBigInt)
    assertTrue(jsRepresentation(jsStr1) == "string:str1")

    // JsString
    val jsBoolTrue: JsBoolean = true.toJsBoolean()
    assertTrue(jsBoolTrue == true.toJsBoolean())
    assertTrue(jsBoolTrue != false.toJsBoolean())
    assertTrue(jsRepresentation(jsBoolTrue) == "boolean:true")

    // JsArray
    val jsArray: JsArray<JsString> = JsArray()
    repeat(3) {
        jsArray[it] = "element$it".toJsString()
    }
    assertTrue(jsArray.length == 3)
    assertTrue(jsArray[1] == "element1".toJsString())
    assertTrue(jsRepresentation(jsArray) == "object:element0,element1,element2")

    // JsReference
    val jsReference: JsReference<Int> = 10.toJsReference()
    assertTrue(jsReference.get() == 10)
    assertTrue(jsReference.toJsReference().get() == jsReference)
    val c = listOf(1)
    assertTrue(c.toJsReference().get() === c.toJsReference().get())
    assertTrue(c.toJsReference() === c.toJsReference())

    // JsBigInt
    val jsBigInt10: JsBigInt = 10L.toJsBigInt()
    val anotherJsBigInt10: JsBigInt = 10L.toJsBigInt()
    assertTrue(jsBigInt10 == anotherJsBigInt10)
    assertTrue(jsBigInt10.toLong() == 10L)
    assertTrue(anotherJsBigInt10.toLong() == 10L)
    val jsBigIntAsJsAny: JsAny = jsBigInt10
    assertTrue(jsBigIntAsJsAny == anotherJsBigInt10)
    assertTrue(jsBigIntAsJsAny is JsBigInt)
    assertTrue(jsBigIntAsJsAny !is JsNumber)
    assertTrue(jsBigIntAsJsAny !is JsString)
    assertTrue(jsBigIntAsJsAny !is JsBoolean)
    assertTrue((jsBigIntAsJsAny as JsBigInt).toLong() == 10L)
    val jsBigIntAsAny: Any = jsBigInt10
    assertTrue(jsBigIntAsAny == anotherJsBigInt10)
    assertTrue(jsBigIntAsAny is JsBigInt)
    assertTrue(jsBigIntAsAny !is JsNumber)
    assertTrue(jsBigIntAsAny !is JsString)
    assertTrue(jsBigIntAsAny !is JsBoolean)
    assertTrue((jsBigIntAsAny as JsBigInt).toLong() == 10L)

    return "OK"
}