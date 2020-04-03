// IGNORE_BACKEND_FIR: JVM_IR

fun useUnit(fn: () -> Unit) {
    fn.invoke()
}

var cInit = false

class C {
    init {
        cInit = true
    }
}

var cWithDefaultInit = false

class CWithDefault(x: Int = 1) {
    init {
        cWithDefaultInit = true
    }
}

var cWithVarargInit = false

class CWithVararg(vararg x: Int) {
    init {
        cWithVarargInit = true
    }
}

fun box(): String {
    useUnit(::C)
    if (!cInit) throw AssertionError("cInit")

    useUnit(::CWithDefault)
    if (!cWithDefaultInit) throw AssertionError("cWithDefaultInit")

    useUnit(::CWithVararg)
    if (!cWithVarargInit) throw AssertionError("cWithVarargInit")

    return "OK"
}