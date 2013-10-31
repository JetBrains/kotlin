public var inc: Int = 0;

public var propInc: Int = 0
    get() {inc++; return $propInc}
    set(a: Int) {
        inc++
        $propInc = a
    }

public var dec: Int = 0;

public var propDec: Int = 0;
    get() { dec--; return $propDec}
    set(a: Int) {
        dec--
        $propDec = a
    }

fun box(): String {
    propInc++
    if (inc != 2) return "fail in postfix increment: ${inc} != 2"
    if (propInc != 1) return "fail in postfix increment: ${propInc} != 1"

    propDec--
    if (dec != -2) return "fail in postfix decrement: ${dec} != -2"
    if (propDec != -1) return "fail in postfix decrement: ${propDec} != -1"

    return "OK"
}