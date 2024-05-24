// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// MODULE: lib
// JVM_DEFAULT_MODE: disable
// FILE: 1.kt
interface KInterface  {
    fun call(): List<String> {
        return Thread.currentThread().getStackTrace().map { it.className + "." + it.methodName }
    }
}

// MODULE: main(lib)
// JVM_DEFAULT_MODE: all
// JVM_TARGET: 1.8
// FILE: main.kt
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
