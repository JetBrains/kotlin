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
    assertTrue((jsNumAsAny as JsNumber).toInt() == 10)
    assertTrue(jsRepresentation(jsNum10) == "number:10")

    // JsString
    val jsStr1: JsString = "str1".toJsString()
    assertTrue(jsStr1 == "str1".toJsString())
    assertTrue(jsStr1 != "str2".toJsString())
    assertTrue(jsStr1.toString() == "str1")
    assertTrue((jsStr1 as Any) is JsString)
    assertTrue((jsStr1 as Any) !is JsNumber)
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

    // JsHandle
    val jsHandle: JsHandle<Int> = 10.toJsHandle()
    assertTrue(jsHandle.get() == 10)
    assertTrue(jsHandle.toJsHandle().get() == jsHandle)
    val c = listOf(1)
    assertTrue(c.toJsHandle().get() === c.toJsHandle().get())
    assertTrue(c.toJsHandle() === c.toJsHandle())

    return "OK"
}