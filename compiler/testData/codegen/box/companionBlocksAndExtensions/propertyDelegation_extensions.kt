// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
object Delegate {
    var value = ""
    operator fun getValue(a: Any?, b: Any?) = value
    operator fun setValue(a: Any?, b: Any?, v: String) {
        value = v
    }
}

object DelegateProvider {
    operator fun provideDelegate(a: Any?, b: Any?) = Delegate
}

class C

companion var C.c by Delegate
companion var C.d by DelegateProvider

fun box(): String {
    C.c = "c"
    if (C.c != "c") return "FAIL 1"

    C.d = "d"
    if (C.d != "d") return "FAIL 2"

    return "OK"
}
