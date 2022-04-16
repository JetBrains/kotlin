// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers

class JSONObject {
    fun build(): JSONObject = TODO()

    fun put(key: String, any: Any): Unit = TODO()
}

fun json(build: JSONObject.() -> Unit) = JSONObject().apply { build() }

context(JSONObject)
infix fun String.by(build: JSONObject.() -> Unit) = put(this, JSONObject().build())

context(JSONObject)
infix fun String.by(value: Any) = put(this, value)

fun test() {
    val json = json {
        "name" by "Kotlin"
        "age" by 10
        "creator" by {
            "name" by "JetBrains"
            "age" by "21"
        }
    }
}