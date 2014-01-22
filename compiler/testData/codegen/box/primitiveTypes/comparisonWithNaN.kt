// This test checks that our bytecode is consistent with javac bytecode

fun assert(condition: Boolean): Unit =
    if (!condition) throw AssertionError("Fail")

fun assertFalse(condition: Boolean) = assert(!condition)

fun box(): String {
    var dnan = java.lang.Double.NaN
    if (System.nanoTime() < 0) dnan = 3.14 // To avoid possible compile-time const propagation

    assertFalse(0.0 < dnan)
    assertFalse(0.0 > dnan)
    assertFalse(0.0 <= dnan)
    assertFalse(0.0 >= dnan)
    assertFalse(0.0 == dnan)
    assertFalse(dnan < 0.0)
    assertFalse(dnan > 0.0)
    assertFalse(dnan <= 0.0)
    assertFalse(dnan >= 0.0)
    assertFalse(dnan == 0.0)
    assertFalse(dnan < dnan)
    assertFalse(dnan > dnan)
    assertFalse(dnan <= dnan)
    assertFalse(dnan >= dnan)
    assertFalse(dnan == dnan)
    
    // Double.compareTo: "NaN is considered by this method to be equal to itself and greater than all other values"
    assert(0.0.compareTo(dnan) == -1)
    assert(dnan.compareTo(0.0) == 1)
    assert(dnan.compareTo(dnan) == 0)

    var fnan = java.lang.Float.NaN
    if (System.nanoTime() < 0) fnan = 3.14f

    assertFalse(0.0f < fnan)
    assertFalse(0.0f > fnan)
    assertFalse(0.0f <= fnan)
    assertFalse(0.0f >= fnan)
    assertFalse(0.0f == fnan)
    assertFalse(fnan < 0.0f)
    assertFalse(fnan > 0.0f)
    assertFalse(fnan <= 0.0f)
    assertFalse(fnan >= 0.0f)
    assertFalse(fnan == 0.0f)
    assertFalse(fnan < fnan)
    assertFalse(fnan > fnan)
    assertFalse(fnan <= fnan)
    assertFalse(fnan >= fnan)
    assertFalse(fnan == fnan)

    assert(0.0.compareTo(fnan) == -1)
    assert(fnan.compareTo(0.0) == 1)
    assert(fnan.compareTo(fnan) == 0)
    
    return "OK"
}
