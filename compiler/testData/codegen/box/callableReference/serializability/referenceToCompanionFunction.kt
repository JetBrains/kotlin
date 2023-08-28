// IGNORE_BACKEND_K1: ANY
// FE 1.0 incorrectly resolves reference `Some::foo` (KT-45315)
// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: Wasm box test does not support disabling only K1 mode with IGNORE_BACKEND directive
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION: KT-61386
// ISSUE: KT-55909
// DUMP_IR

abstract class Base {
    fun foo(): String = "A"
    abstract fun bar(): String
}

class Some {
    companion object : Base() {
        override fun bar(): String = "B"
    }
}

// For sanity check
object Singleton : Base() {
    override fun bar(): String = "C"
}

fun box(): String {
    val ref1 = Some::foo
    val ref2 = Some::bar
    val ref3 = Some.Companion::foo
    val ref4 = Some.Companion::bar
    val ref5 = Singleton::foo
    val ref6 = Singleton::bar
    val result = ref1() + ref2() + ref3() + ref4() + ref5() + ref6()
    return if (result == "ABABAC") "OK" else "Fail: $result"
}
