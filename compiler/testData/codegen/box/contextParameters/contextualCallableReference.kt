// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// IGNORE_BACKEND: JVM_IR, NATIVE
// ^KT-86452, KT-87390

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

fun box(): String {
    // Tests on invocation of callables:
    context("O", 'K') {
        val fnRef: () -> String = O::foo
        fnRef().let { result ->
            if (result != "OK") return "FAIL 1: $result != \"OK\""
        }

        val propRef = O::bar
        propRef().let { result ->
            if (result != "") return "FAIL 2: $result != \"\""
        }

        propRef.set("K")
        propRef().let { result ->
            if (result != "OK") return "FAIL 3: $result != \"OK\""
        }
    }

    // Tests on getting reference name:
    context("O", 'K') {
        val fnRef = O::foo
        if (fnRef.name != "foo") return "FAIL 4: ${fnRef.name} != \"foo\""

        val propRef = O::bar
        if (propRef.name != "bar") return "FAIL 5: ${propRef.name} != \"bar\""
    }

    return "OK"
}
