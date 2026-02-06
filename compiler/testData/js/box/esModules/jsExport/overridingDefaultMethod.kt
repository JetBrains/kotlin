// KT-68067
// ES_MODULES
// FILE: api.kt

@JsExport
open class BaseClass {
    open fun sayHello(name: String? = "NN") : String {
        return "Hello, $name from BaseClass"
    }
}

@JsExport
class SubClass : BaseClass() {
    override fun sayHello(name: String?): String {
        val base = super.sayHello(name)
        return "$base, and from SubClass"
    }
}

// FILE: main.kt
external interface JsResult {
    val baseClassResult: String
    val subClassResult: String
}

@JsModule("./overridingDefaultMethod.mjs")
external fun jsBox(): JsResult

fun box(): String {
    val res = jsBox()
    val firstResultExpectation = "Hello, NN from BaseClass"
    val secondResultExpectation = "Hello, NN from BaseClass, and from SubClass"

    if (res.baseClassResult != firstResultExpectation) {
        return "Fail: '${res.baseClassResult}' is not expected, expected '$firstResultExpectation'"
    }

    if (res.subClassResult != secondResultExpectation) {
        return "Fail: '${res.subClassResult}' is not expected, expected '$secondResultExpectation'"
    }

    return "OK"
}
