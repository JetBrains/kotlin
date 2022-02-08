// !JVM_DEFAULT_MODE: disable
// TARGET_BACKEND: JVM

// First item on Android is `java.lang.Thread.getStackTrace`
// IGNORE_BACKEND: ANDROID

// WITH_STDLIB
// FULL_JDK

interface Test {
    fun call(): List<String> = Thread.currentThread().getStackTrace().map { it.className + "." + it.methodName }
}

interface A : Test
interface B : Test

interface C: B, A

class Foo : C
class Foo2 : A, B, C

fun box(): String {
    var result = Foo().call()
    if (result[1] != "Test\$DefaultImpls.call") return "fail 1: ${result[1]}"
    if (result[2] != "B\$DefaultImpls.call") return "fail 2: ${result[2]}"
    if (result[3] != "C\$DefaultImpls.call") return "fail 3: ${result[3]}"
    if (result[4] != "Foo.call") return "fail 4: ${result[4]}"
    if (result[5] != "DefaultImplCallKt.box") return "fail 6: ${result[5]}"

    result = Foo2().call()
    if (result[1] != "Test\$DefaultImpls.call") return "fail 7: ${result[1]}"
    if (result[2] != "A\$DefaultImpls.call") return "fail 8: ${result[2]}"
    if (result[3] != "Foo2.call") return "fail 9: ${result[3]}"
    if (result[4] != "DefaultImplCallKt.box") return "fail 10: ${result[4]}"

    return "OK"
}
