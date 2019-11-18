// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// This test checks that our bytecode is consistent with javac bytecode

fun _assert(condition: Boolean) {
    if (!condition) throw AssertionError("Fail")
}

fun _assertFalse(condition: Boolean) = _assert(!condition)

fun box(): String {
    var dnan = java.lang.Double.NaN
    if (System.nanoTime() < 0) dnan = 3.14 // To avoid possible compile-time const propagation

    _assertFalse(0.0 < dnan)
    _assertFalse(0.0 > dnan)
    _assertFalse(0.0 <= dnan)
    _assertFalse(0.0 >= dnan)
    _assertFalse(0.0 == dnan)
    _assertFalse(dnan < 0.0)
    _assertFalse(dnan > 0.0)
    _assertFalse(dnan <= 0.0)
    _assertFalse(dnan >= 0.0)
    _assertFalse(dnan == 0.0)
    _assertFalse(dnan < dnan)
    _assertFalse(dnan > dnan)
    _assertFalse(dnan <= dnan)
    _assertFalse(dnan >= dnan)
    _assertFalse(dnan == dnan)
    
    // Double.compareTo: "NaN is considered by this method to be equal to itself and greater than all other values"
    _assert(0.0.compareTo(dnan) == -1)
    _assert(dnan.compareTo(0.0) == 1)
    _assert(dnan.compareTo(dnan) == 0)

    var fnan = java.lang.Float.NaN
    if (System.nanoTime() < 0) fnan = 3.14f

    _assertFalse(0.0f < fnan)
    _assertFalse(0.0f > fnan)
    _assertFalse(0.0f <= fnan)
    _assertFalse(0.0f >= fnan)
    _assertFalse(0.0f == fnan)
    _assertFalse(fnan < 0.0f)
    _assertFalse(fnan > 0.0f)
    _assertFalse(fnan <= 0.0f)
    _assertFalse(fnan >= 0.0f)
    _assertFalse(fnan == 0.0f)
    _assertFalse(fnan < fnan)
    _assertFalse(fnan > fnan)
    _assertFalse(fnan <= fnan)
    _assertFalse(fnan >= fnan)
    _assertFalse(fnan == fnan)

    _assert(0.0.compareTo(fnan) == -1)
    _assert(fnan.compareTo(0.0) == 1)
    _assert(fnan.compareTo(fnan) == 0)
    
    return "OK"
}
