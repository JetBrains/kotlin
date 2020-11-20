// IGNORE_BACKEND_FIR: JVM_IR
// FULL_JDK
// FILE: 1.kt
// !JVM_DEFAULT_MODE: disable
interface KInterface  {
    fun call(): List<String> {
        return Thread.currentThread().getStackTrace().map { it.className + "." + it.methodName }
    }
}

// FILE: main.kt
// !JVM_DEFAULT_MODE: all
// JVM_TARGET: 1.8
interface KInterface2 : KInterface  {

}

class Foo: KInterface2 {

}

fun box(): String {
    var result = Foo().call()
    if (result[1] != "KInterface\$DefaultImpls.call") return "fail 1: ${result[1]}"
    if (result[2] != "KInterface2\$DefaultImpls.call") return "fail 2: ${result[2]}"
    if (result[3] != "Foo.call") return "fail 3: ${result[3]}"
    if (result[4] != "MainKt.box") return "fail 4: ${result[4]}"

    return "OK"
}
