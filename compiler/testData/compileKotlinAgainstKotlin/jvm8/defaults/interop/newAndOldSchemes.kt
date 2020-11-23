// IGNORE_BACKEND_FIR: JVM_IR
// FULL_JDK
// FILE: 1.kt
interface KInterface  {
    fun call(): List<String> {
        return Thread.currentThread().getStackTrace().map { it.className + "." + it.methodName }
    }

    fun superCall()  = Thread.currentThread().getStackTrace().map { it.className + "." + it.methodName }
}

// FILE: main.kt
// !JVM_DEFAULT_MODE: all-compatibility
// JVM_TARGET: 1.8
interface KInterface2 : KInterface  {
    override fun superCall() = super.superCall()
}

interface KInterface3 : KInterface2  {

}

class Foo: KInterface3 {
    fun superCall2() = super<KInterface3>.superCall()
}

fun box(): String {
    var result = Foo().call()
    if (result[1] != "KInterface\$DefaultImpls.call") return "fail 1: ${result[1]}"
    if (result[2] != "KInterface2\$DefaultImpls.call") return "fail 2: ${result[2]}"
    if (result[3] != "KInterface3\$DefaultImpls.call") return "fail 3: ${result[3]}"
    if (result[4] != "Foo.call") return "fail 4: ${result[4]}"
    if (result[5] != "MainKt.box") return "fail 5: ${result[5]}"

    result = Foo().superCall2()
    if (result[1] != "KInterface\$DefaultImpls.superCall") return "fail 1: ${result[1]}"
    if (result[2] != "KInterface2.superCall") return "fail 2: ${result[2]}"
    if (result[3] != "Foo.superCall2") return "fail 3: ${result[3]}"
    if (result[4] != "MainKt.box") return "fail 4: ${result[4]}"

    return "OK"
}
