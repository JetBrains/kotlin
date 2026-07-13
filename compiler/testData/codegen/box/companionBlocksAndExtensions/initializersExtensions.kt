// LANGUAGE: +CompanionBlocksAndExtensions

var initialized = false
class Foo() {
    companion {
        val staticProp = run { initialized = true; "" }
    }
}

companion fun Foo.sayHi(): String { return "hi" }
companion val Foo.greeting: String = "hi"

fun box(): String {
    val hi = Foo.sayHi()
    if (hi != "hi") return "FAIL: hi=${hi}"

    val greeting = Foo.greeting
    if (greeting != "hi") return "FAIL: Foo.greeting=${greeting}"

    // Companion extension access should not trigger initializers:
    // §3.4 (companion extensions): we remark that calling a companion extension does not imply the initialization of the classifier being extended
    if (initialized) return "FAIL: initialized=${initialized}"
    return "OK"
}
