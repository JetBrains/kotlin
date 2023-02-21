// IGNORE_BACKEND_K1: ANY
// Behavior changed in K2, see KT-42077
// IGNORE_BACKEND: WASM
// SKIP_NODE_JS

public var inc: Int = 0

public var propInc: Int = 0
    get() {++inc; return field}
    set(a: Int) {
        ++inc
        field = a
    }

public var dec: Int = 0

public var propDec: Int = 0
    get() { --dec; return field}
    set(a: Int) {
        --dec
        field = a
    }

fun box(): String {
    ++propInc
    if (inc != 2) return "fail in prefix increment: ${inc} != 2"
    if (propInc != 1) return "fail in prefix increment: ${propInc} != 1"

    --propDec
    if (dec != -2) return "fail in prefix decrement: ${dec} != -2"
    if (propDec != -1) return "fail in prefix decrement: ${propDec} != -1"

    return "OK"
}
