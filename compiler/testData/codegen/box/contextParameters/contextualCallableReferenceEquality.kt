// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// IGNORE_BACKEND: JVM_IR, WASM_JS, WASM_WASI
// ^KT-86452, KT-87390

import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty0

object O {
    context(s: String, c: Char)
    fun foo(default: Int = 0) = s + c
}

var _x = ""

context(s: String)
var O.bar: String
    get() = _x
    set(v) {
        _x = s + v
    }

fun test() = Unit

fun box(): String {
    context("O", 'K') {
        val ref1 = O::foo
        val ref2 = O::foo
        val ref3: (Int) -> String = O::foo
        val ref4: (Int) -> String = O::foo
        val ref5: KFunction<String> = O::foo
        val ref6: KFunction<String> = O::foo

        val ref1HashCode = ref1.hashCode()
        val ref2HashCode = ref2.hashCode()
        val ref3HashCode = ref3.hashCode()
        val ref4HashCode = ref4.hashCode()
        val ref5HashCode = ref5.hashCode()
        val ref6HashCode = ref6.hashCode()

        if (ref1HashCode != ref2HashCode) return "FAIL 1: $ref1HashCode != $ref2HashCode"
        if (ref1HashCode != ref3HashCode) return "FAIL 2: $ref1HashCode != $ref3HashCode"
        if (ref1HashCode != ref4HashCode) return "FAIL 3: $ref1HashCode != $ref4HashCode"
        if (ref1HashCode != ref5HashCode) return "FAIL 4: $ref1HashCode != $ref5HashCode"
        if (ref1HashCode != ref6HashCode) return "FAIL 5: $ref1HashCode != $ref6HashCode"

        if (ref1 != ref2) return "FAIL 6: $ref1 != $ref2"
        if (ref1 != ref3) return "FAIL 7: $ref1 != $ref3"
        if (ref1 != ref4) return "FAIL 8: $ref1 != $ref4"
        if (ref1 != ref5) return "FAIL 9: $ref1 != $ref5"
        if (ref1 != ref6) return "FAIL 10: $ref1 != $ref6"
    }

    run {
        val ref1 = context("O", 'K') { O::foo }
        val ref2 = context("O", 'K') { O::foo }
        val ref3: (Int) -> String = context("O", 'K') { O::foo as ((Int) -> String) }
        val ref4: (Int) -> String = context("O", 'K') { O::foo as ((Int) -> String) }
        val ref5: KFunction<String> = context("O", 'K') { O::foo }
        val ref6: KFunction<String> = context("O", 'K') { O::foo }

        val ref7 = context("A", 'B') { O::foo }
        val ref8: (Int) -> String = context("A", 'B') { O::foo as ((Int) -> String) }
        val ref9: KFunction<String> = context("A", 'B') { O::foo }

        val ref1HashCode = ref1.hashCode()
        val ref2HashCode = ref2.hashCode()
        val ref3HashCode = ref3.hashCode()
        val ref4HashCode = ref4.hashCode()
        val ref5HashCode = ref5.hashCode()
        val ref6HashCode = ref6.hashCode()
        val ref7HashCode = ref7.hashCode()
        val ref8HashCode = ref8.hashCode()
        val ref9HashCode = ref9.hashCode()

        if (ref1HashCode != ref2HashCode) return "FAIL 11: $ref1HashCode != $ref2HashCode"
        if (ref1HashCode != ref3HashCode) return "FAIL 12: $ref1HashCode != $ref3HashCode"
        if (ref1HashCode != ref4HashCode) return "FAIL 13: $ref1HashCode != $ref4HashCode"
        if (ref1HashCode != ref5HashCode) return "FAIL 14: $ref1HashCode != $ref5HashCode"
        if (ref1HashCode != ref6HashCode) return "FAIL 15: $ref1HashCode != $ref6HashCode"
        if (ref1HashCode == ref7HashCode) return "FAIL 16: $ref1HashCode == $ref7HashCode"
        if (ref1HashCode == ref8HashCode) return "FAIL 17: $ref1HashCode == $ref8HashCode"
        if (ref1HashCode == ref9HashCode) return "FAIL 18: $ref1HashCode == $ref9HashCode"

        if (ref1 != ref2) return "FAIL 19: $ref1 != $ref2"
        if (ref1 != ref3) return "FAIL 20: $ref1 != $ref3"
        if (ref1 != ref4) return "FAIL 21: $ref1 != $ref4"
        if (ref1 != ref5) return "FAIL 22: $ref1 != $ref5"
        if (ref1 != ref6) return "FAIL 23: $ref1 != $ref6"
        if (ref1 == ref7) return "FAIL 24: $ref1 == $ref7"
        if (ref1 == ref8) return "FAIL 25: $ref1 == $ref8"
        if (ref1 == ref9) return "FAIL 26: $ref1 == $ref9"
    }

    context("O", 'K') {
        val ref1 = O::bar
        val ref2 = O::bar
        val ref3: KMutableProperty0<String> = O::bar
        val ref4: KMutableProperty0<String> = O::bar

        val ref1HashCode = ref1.hashCode()
        val ref2HashCode = ref2.hashCode()
        val ref3HashCode = ref3.hashCode()
        val ref4HashCode = ref4.hashCode()

        if (ref1HashCode != ref2HashCode) return "FAIL 27: $ref1HashCode != $ref2HashCode"
        if (ref1HashCode != ref3HashCode) return "FAIL 28: $ref1HashCode != $ref3HashCode"
        if (ref1HashCode != ref4HashCode) return "FAIL 29: $ref1HashCode != $ref4HashCode"

        if (ref1 != ref2) return "FAIL 30: $ref1 != $ref2"
        if (ref1 != ref3) return "FAIL 31: $ref1 != $ref3"
        if (ref1 != ref4) return "FAIL 32: $ref1 != $ref4"
    }

    run {
        val ref1 = context("O", 'K') { O::bar}
        val ref2 = context("O", 'K') { O::bar }
        val ref3: KMutableProperty0<String> = context("O", 'K') { O::bar }
        val ref4: KMutableProperty0<String> = context("O", 'K') { O::bar }

        val ref5 = context("A", 'B') { O::bar }
        val ref6: KMutableProperty0<String> = context("A", 'B') { O::bar }

        val ref1HashCode = ref1.hashCode()
        val ref2HashCode = ref2.hashCode()
        val ref3HashCode = ref3.hashCode()
        val ref4HashCode = ref4.hashCode()
        val ref5HashCode = ref5.hashCode()
        val ref6HashCode = ref6.hashCode()

        if (ref1HashCode != ref2HashCode) return "FAIL 33: $ref1HashCode != $ref2HashCode"
        if (ref1HashCode != ref3HashCode) return "FAIL 34: $ref1HashCode != $ref3HashCode"
        if (ref1HashCode != ref4HashCode) return "FAIL 35: $ref1HashCode != $ref4HashCode"
        if (ref1HashCode == ref5HashCode) return "FAIL 36: $ref1HashCode == $ref5HashCode"
        if (ref1HashCode == ref6HashCode) return "FAIL 37: $ref1HashCode == $ref6HashCode"

        if (ref1 != ref2) return "FAIL 38: $ref1 != $ref2"
        if (ref1 != ref3) return "FAIL 39: $ref1 != $ref3"
        if (ref1 != ref4) return "FAIL 40: $ref1 != $ref4"
        if (ref1 == ref5) return "FAIL 41: $ref1 == $ref5"
        if (ref1 == ref6) return "FAIL 42: $ref1 == $ref6"
    }

    return "OK"
}
