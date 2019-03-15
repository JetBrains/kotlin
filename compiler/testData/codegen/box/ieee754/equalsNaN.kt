// !LANGUAGE: -ProperIeee754Comparisons
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

import kotlin.test.*

object O {
    var equalsCalled: Boolean = false
        get(): Boolean {
            val result = field
            field = false
            return result
        }
        set(v: Boolean) {
            field = v
        }

    override fun equals(a: Any?): Boolean {
        equalsCalled = true
        return true
    }
}

val A: Any = O

fun <T: Double> testDouble(d: Double, v: T, vararg va: T) {
    assertFalse(d == d, "Double: d != d")
    assertFalse(d == v, "Double: d != v")
    assertFalse(d == va[0], "Double: d != va[0]")
    assertFalse(v == d, "Double: v != d")
    assertFalse(v == v, "Double: v != v")
    assertFalse(v == va[0], "Double: v != va[0]")
    assertFalse(va[0] == d, "Double: va[0] != d")
    assertFalse(va[0] == v, "Double: va[0] != v")
    assertFalse(va[0] == va[0], "Double: va[0] != va[0]")

    assertTrue(d != d, "Double: d == d")
    assertTrue(d != v, "Double: d == v")
    assertTrue(d != va[0], "Double: d == va[0]")
    assertTrue(v != d, "Double: v == d")
    assertTrue(v != v, "Double: v == v")
    assertTrue(v != va[0], "Double: v == va[0]")
    assertTrue(va[0] != d, "Double: va[0] == d")
    assertTrue(va[0] != v, "Double: va[0] == v")
    assertTrue(va[0] != va[0], "Double: va[0] == va[0]")
}

fun <T: Float> testFloat(d: Float, v: T, vararg va: T) {
    assertFalse(d == d, "Float: d != d")
    assertFalse(d == v, "Float: d != v")
    assertFalse(d == va[0], "Float: d != va[0]")
    assertFalse(v == d, "Float: v != d")
    assertFalse(v == v, "Float: v != v")
    assertFalse(v == va[0], "Float: v != va[0]")
    assertFalse(va[0] == d, "Float: va[0] != d")
    assertFalse(va[0] == v, "Float: va[0] != v")
    assertFalse(va[0] == va[0], "Float: va[0] != va[0]")

    assertTrue(d != d, "Float: d == d")
    assertTrue(d != v, "Float: d == v")
    assertTrue(d != va[0], "Float: d == va[0]")
    assertTrue(v != d, "Float: v == d")
    assertTrue(v != v, "Float: v == v")
    assertTrue(v != va[0], "Float: v == va[0]")
    assertTrue(va[0] != d, "Float: va[0] == d")
    assertTrue(va[0] != v, "Float: va[0] == v")
    assertTrue(va[0] != va[0], "Float: va[0] == va[0]")
}

var gdn: Any = Double.NaN
var gfn: Any = Float.NaN

fun box(): String {

    // Double

    val dn = Double.NaN
    val adn: Any = dn
    val dnq: Double? = dn
    val adnq: Any? = dn

    // see https://bugs.openjdk.java.net/browse/JDK-8141407
    val nanBug = dnq == dnq

    assertFalse(dn == dn, "Double: NaN == NaN")
    assertTrue(dn == adn, "Double: NaN != (Any)NaN")
    assertTrue(adn == dn, "Double: (Any)NaN != NaN")
    assertTrue(adn == adn, "Double: (Any)NaN != (Any)NaN")

    assertFalse(dn == dnq, "Double: NaN == NaN?")
    assertTrue(dn == adnq, "Double: NaN != (Any?)NaN")
    assertTrue(adn == dnq, "Double: (Any)NaN != NaN?")
    assertTrue(adn == adnq, "Double: (Any)NaN != (Any?)NaN")

    assertFalse(dnq == dn, "Double: NaN? == NaN")
    assertTrue(dnq == adn, "Double: NaN? != (Any)NaN")
    assertTrue(adnq == dn, "Double: (Any?)NaN != NaN")
    assertTrue(adnq == adn, "Double: (Any?)NaN != (Any)NaN")

    if (!nanBug) assertFalse(dnq == dnq, "Double: NaN? == NaN?")
    assertTrue(dnq == adnq, "Double: NaN? != (Any?)NaN")
    assertTrue(adnq == dnq, "Double: (Any?)NaN != NaN?")
    assertTrue(adnq == adnq, "Double: (Any?)NaN != (Any?)NaN")

    assertTrue(dn != dn, "Double: NaN == NaN")
    assertFalse(dn != adn, "Double: NaN != (Any)NaN")
    assertFalse(adn != dn, "Double: (Any)NaN != NaN")
    assertFalse(adn != adn, "Double: (Any)NaN != (Any)NaN")

    assertTrue(dn != dnq, "Double: NaN == NaN?")
    assertFalse(dn != adnq, "Double: NaN != (Any?)NaN")
    assertFalse(adn != dnq, "Double: (Any)NaN != NaN?")
    assertFalse(adn != adnq, "Double: (Any)NaN != (Any?)NaN")

    assertTrue(dnq != dn, "Double: NaN? == NaN")
    assertFalse(dnq != adn, "Double: NaN? != (Any)NaN")
    assertFalse(adnq != dn, "Double: (Any?)NaN != NaN")
    assertFalse(adnq != adn, "Double: (Any?)NaN != (Any)NaN")

    if (!nanBug) assertTrue(dnq != dnq, "Double: NaN? == NaN?")
    assertFalse(dnq != adnq, "Double: NaN? != (Any?)NaN")
    assertFalse(adnq != dnq, "Double: (Any?)NaN != NaN?")
    assertFalse(adnq != adnq, "Double: (Any?)NaN != (Any?)NaN")

    // Stable smart-casts -- effectively not takein into account in 1.2
    if (adn is Double) {
        assertTrue(adn == adn, "Double smart-cast: NaN == NaN")
        assertFalse(adn != adn, "Double smart-cast: NaN == NaN")
    }
    if (adnq is Double?) {
        assertTrue(adnq == adnq, "Double? smart-cast: NaN? == NaN?")
        assertFalse(adnq != adnq, "Double? smart-cast: NaN? == NaN?")
    }
    // Unstable smart-casts
    if (gdn is Double) {
        assertTrue(gdn == gdn, "Unstable Double smart-cast: NaN != NaN")
        assertFalse(gdn != gdn, "Unstable Double smart-cast: NaN != NaN")
    }
    if (gdn is Double?) {
        assertTrue(gdn == gdn, "Unstable Double smart-cast: NaN != NaN")
        assertFalse(gdn != gdn, "Unstable Double smart-cast: NaN != NaN")
    }

    // Explicit .equals
    assertTrue(A == dn && O.equalsCalled, "A.equals not called for A == dn")
    assertTrue(dn != A && !O.equalsCalled, "A.equals called for dn == A")
    assertFalse(A != dn || !O.equalsCalled, "A.equals not called for A != dn")
    assertFalse(dn == A || O.equalsCalled, "A.equals called for dn != A")

    // Generics and varags
    testDouble(Double.NaN, Double.NaN, Double.NaN)

    // Float

    val fn = Float.NaN
    val afn: Any = fn
    val fnq: Float? = fn
    val afnq: Any? = fn

    assertFalse(fn == fn, "Float: NaN == NaN")
    assertTrue(fn == afn, "Float: NaN != (Any)NaN")
    assertTrue(afn == fn, "Float: (Any)NaN != NaN")
    assertTrue(afn == afn, "Float: (Any)NaN != (Any)NaN")

    assertFalse(fn == fnq, "Float: NaN == NaN?")
    assertTrue(fn == afnq, "Float: NaN != (Any?)NaN")
    assertTrue(afn == fnq, "Float: (Any)NaN != NaN?")
    assertTrue(afn == afnq, "Float: (Any)NaN != (Any?)NaN")

    assertFalse(fnq == fn, "Float: NaN? == NaN")
    assertTrue(fnq == afn, "Float: NaN? != (Any)NaN")
    assertTrue(afnq == fn, "Float: (Any?)NaN != NaN")
    assertTrue(afnq == afn, "Float: (Any?)NaN != (Any)NaN")

    if (!nanBug) assertFalse(fnq == fnq, "Float: NaN? == NaN?")
    assertTrue(fnq == afnq, "Float: NaN? != (Any?)NaN")
    assertTrue(afnq == fnq, "Float: (Any?)NaN != NaN?")
    assertTrue(afnq == afnq, "Float: (Any?)NaN != (Any?)NaN")

    assertTrue(fn != fn, "Float: NaN == NaN")
    assertFalse(fn != afn, "Float: NaN != (Any)NaN")
    assertFalse(afn != fn, "Float: (Any)NaN != NaN")
    assertFalse(afn != afn, "Float: (Any)NaN != (Any)NaN")

    assertTrue(fn != fnq, "Float: NaN == NaN?")
    assertFalse(fn != afnq, "Float: NaN != (Any?)NaN")
    assertFalse(afn != fnq, "Float: (Any)NaN != NaN?")
    assertFalse(afn != afnq, "Float: (Any)NaN != (Any?)NaN")

    assertTrue(fnq != fn, "Float: NaN? == NaN")
    assertFalse(fnq != afn, "Float: NaN? != (Any)NaN")
    assertFalse(afnq != fn, "Float: (Any?)NaN != NaN")
    assertFalse(afnq != afn, "Float: (Any?)NaN != (Any)NaN")

    if (!nanBug) assertTrue(fnq != fnq, "Float: NaN? == NaN?")
    assertFalse(fnq != afnq, "Float: NaN? != (Any?)NaN")
    assertFalse(afnq != fnq, "Float: (Any?)NaN != NaN?")
    assertFalse(afnq != afnq, "Float: (Any?)NaN != (Any?)NaN")

    // Stable smart-casts -- effectively not takein into account in 1.2
    if (afn is Float) {
        assertTrue(afn == afn, "Float smart-cast: NaN == NaN")
        assertFalse(afn != afn, "Float smart-cast: NaN == NaN")
    }
    if (afnq is Float?) {
        assertTrue(afnq == afnq, "Float? smart-cast: NaN? == NaN?")
        assertFalse(afnq != afnq, "Float? smart-cast: NaN? == NaN?")
    }
    // Unstable smart-casts
    if (gfn is Float) {
        assertTrue(gfn == gfn, "Unstable Float smart-cast: NaN != NaN")
        assertFalse(gfn != gfn, "Unstable Float smart-cast: NaN != NaN")
    }
    if (gfn is Float?) {
        assertTrue(gfn == gfn, "Unstable Float smart-cast: NaN != NaN")
        assertFalse(gfn != gfn, "Unstable Float smart-cast: NaN != NaN")
    }

    assertTrue(A == fn && O.equalsCalled, "A.equals not called for A == fn")
    assertTrue(fn != A && !O.equalsCalled, "A.equals called for fn == A")
    assertFalse(A != fn || !O.equalsCalled, "A.equals not called for A != fn")
    assertFalse(fn == A || O.equalsCalled, "A.equals called for fn != A")

    // Generics and varags
    testFloat(Float.NaN, Float.NaN, Float.NaN)

    return "OK"
}