// !JVM_DEFAULT_MODE: all
// FULL_JDK
// WITH_STDLIB
// JVM_TARGET: 1.8
// MODULE: lib
// FILE: 1.kt
interface KInterface  {
    fun call(): List<String> {
        return Thread.currentThread().getStackTrace().map { it.className + "." + it.methodName }
    }

    fun superCall()  = Thread.currentThread().getStackTrace().map { it.className + "." + it.methodName }
}

// MODULE: main(lib)
// FILE: main.kt
interface KInterface2 : KInterface  {

}

class Foo: KInterface2 {
    fun superCall2() = super<KInterface2>.superCall()
}

fun box(): String {
    var result = Foo().call()
    if (result[1] != "KInterface.call") return "fail 1: ${result[1]}"
    if (result[2] != "MainKt.box") return "fail 2: ${result[2]}"

    result = Foo().superCall2()
    if (result[1] != "KInterface.superCall") return "fail 1: ${result[1]}"
    if (result[2] != "Foo.superCall2") return "fail 2: ${result[2]}"
    if (result[3] != "MainKt.box") return "fail 3: ${result[3]}"


    return "OK"
}
