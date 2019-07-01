// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

annotation class Ann(val value: String)

@Ann("OK")
val property: String
    get() = ""

fun box(): String {
    return (::property.annotations.single() as Ann).value
}
