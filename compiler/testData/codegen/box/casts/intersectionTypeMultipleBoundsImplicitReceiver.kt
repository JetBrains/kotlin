// IGNORE_BACKEND_FIR: JVM_IR
interface FirstTrait
interface SecondTrait

fun <T> T.doSomething(): String where T : FirstTrait, T : SecondTrait {
    return "OK"
}

class Foo : FirstTrait, SecondTrait {
    fun bar(): String {
        return doSomething()
    }
}

fun box(): String {
    return Foo().bar()
}