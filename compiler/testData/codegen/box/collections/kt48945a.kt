// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// IGNORE_BACKEND_K2: JVM_IR, JS_IR
// FIR status: different structure of fake overrides. Fixed in the IR fake override builder.
// IGNORE_BACKEND: ANDROID
//  ^ NSME: java.util.AbstractMap.remove
// FULL_JDK
interface MSS : Map<String, String>

class Test : MSS, java.util.AbstractMap<String, String>() {
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = throw Exception()
}

fun box(): String {
    Test().remove(null, "")
    return "OK"
}
