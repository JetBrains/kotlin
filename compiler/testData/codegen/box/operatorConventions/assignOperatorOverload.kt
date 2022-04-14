// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM_IR

var result: String = "Fail"

operator fun Any.assign(other: String) {
    result = other
}

operator fun Container.assign(other: Int) {
    this.value = "OK"
}

data class Container(var value: String)

data class Foo(val x: Container)

fun box(): String {
    val x = 10
    x = "OK"
    if (result != "OK") return "Fail: $result"

    val foo = Foo(Container("Fail"))
    foo.x = 42
    return foo.x.value
}
