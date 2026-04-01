// JVM_DEFAULT_MODE: disable
// TARGET_BACKEND: JVM

// FULL_JDK

package test

interface Test {
    fun call(): List<String> {
        val stackTrace = Thread.currentThread().getStackTrace()
        // On Android, first frame is [dalvik.system.VMStack.getThreadStackTrace]
        if (stackTrace[0].methodName == "getThreadStackTrace") return stackTrace.drop(1).map { it.className + "." + it.methodName }
        return stackTrace.map { it.className + "." + it.methodName }
    }
}

interface A : Test
interface B : Test

interface C: B, A

class Foo : C
class Foo2 : A, B, C

fun box(): String {
    var result = Foo().call()
    if (result[1] != "test.Test\$DefaultImpls.call") return "fail 1: ${result[1]}, ${result}"
    if (result[2] != "test.B\$DefaultImpls.call") return "fail 2: ${result[2]}, ${result}"
    if (result[3] != "test.C\$DefaultImpls.call") return "fail 3: ${result[3]}, ${result}"
    if (result[4] != "test.Foo.call") return "fail 4: ${result[4]}, ${result}"
    if (result[5] != "test.DefaultImplCallKt.box") return "fail 6: ${result[5]}, ${result}"

    result = Foo2().call()
    if (result[1] != "test.Test\$DefaultImpls.call") return "fail 7: ${result[1]}, ${result}"
    if (result[2] != "test.A\$DefaultImpls.call") return "fail 8: ${result[2]}, ${result}"
    if (result[3] != "test.Foo2.call") return "fail 9: ${result[3]}, ${result}"
    if (result[4] != "test.DefaultImplCallKt.box") return "fail 10: ${result[4]}, ${result}"

    return "OK"
}
