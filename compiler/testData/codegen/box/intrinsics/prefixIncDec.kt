public var inc: Int = 0;

public var propInc: Int = 0
    get() {++inc; return $propInc}
    set(a: Int) {
        ++inc
        $propInc = a
    }

public var dec: Int = 0;

public var propDec: Int = 0;
    get() { --dec; return $propDec}
    set(a: Int) {
        --dec
        $propDec = a
    }

fun box(): String {
    ++propInc
    if (inc != 3) return "fail in prefix increment: ${inc} != 3"
    if (propInc != 1) return "fail in prefix increment: ${propInc} != 1"

    --propDec
    if (dec != -3) return "fail in prefix decrement: ${dec} != -3"
    if (propDec != -1) return "fail in prefix decrement: ${propDec} != -1"

    return "OK"
}