
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses
// !DIAGNOSTICS: -EQUALITY_NOT_APPLICABLE

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class I1

value object O1: I1()

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class I2: I1()

value object O2: I2()

OPTIONAL_JVM_INLINE_ANNOTATION
value class I3(val value: String): I2()


fun <T> boxValue(v: T): T = v

fun <T: I1> coerceToI1(v: T): I1 = v

fun <T: I2> coerceToI2(v: T): I2 = v

fun <T> coerceToAny(v: T): Any? = v

@Suppress("EQUALITY_NOT_APPLICABLE")
fun box(): String {
    val i3 = I3("A")

    if (i3 == O1) return "FAIL 111"
    if (i3 == O2) return "FAIL 121"
    if (i3 != i3) return "FAIL 131"

    if (boxValue(i3) == O1) return "FAIL 112"
    if (boxValue(i3) == O2) return "FAIL 122"
    if (boxValue(i3) != i3) return "FAIL 132"
    if (i3 == boxValue(O1)) return "FAIL 142"
    if (i3 == boxValue(O2)) return "FAIL 152"
    if (i3 != boxValue(i3)) return "FAIL 162"
    if (boxValue(i3) == boxValue(O1)) return "FAIL 172"
    if (boxValue(i3) == boxValue(O2)) return "FAIL 182"
    if (boxValue(i3) != boxValue(i3)) return "FAIL 192"

    if (coerceToI2(i3) == O1) return "FAIL 113"
    if (coerceToI2(i3) == O2) return "FAIL 123"
    if (coerceToI2(i3) != i3) return "FAIL 133"
    if (i3 == coerceToI2(O2)) return "FAIL 153"
    if (i3 != coerceToI2(i3)) return "FAIL 163"
    if (coerceToI2(i3) == coerceToI2(O2)) return "FAIL 173"
    if (coerceToI2(i3) != coerceToI2(i3)) return "FAIL 183"

    if (coerceToI1(i3) == O1) return "FAIL 114"
    if (coerceToI1(i3) == O2) return "FAIL 124"
    if (coerceToI1(i3) != i3) return "FAIL 134"
    if (i3 == coerceToI1(O1)) return "FAIL 144"
    if (i3 == coerceToI1(O2)) return "FAIL 154"
    if (i3 != coerceToI1(i3)) return "FAIL 164"
    if (coerceToI1(i3) == coerceToI1(O1)) return "FAIL 174"
    if (coerceToI1(i3) == coerceToI1(O2)) return "FAIL 184"
    if (coerceToI1(i3) != coerceToI1(i3)) return "FAIL 194"

    if (coerceToAny(i3) == O1) return "FAIL 115"
    if (coerceToAny(i3) == O2) return "FAIL 125"
    if (coerceToAny(i3) != i3) return "FAIL 135"
    if (i3 == coerceToAny(O1)) return "FAIL 145"
    if (i3 == coerceToAny(O2)) return "FAIL 155"
    if (i3 != coerceToAny(i3)) return "FAIL 165"
    if (coerceToAny(i3) == coerceToAny(O1)) return "FAIL 175"
    if (coerceToAny(i3) == coerceToAny(O2)) return "FAIL 185"
    if (coerceToAny(i3) != coerceToAny(i3)) return "FAIL 195"

    if (O1 != O1) return "FAIL 211"
    if (O1 == O2) return "FAIL 221"
    if (O1 == i3) return "FAIL 231"

    if (boxValue(O1) != O1) return "FAIL 212"
    if (boxValue(O1) == O2) return "FAIL 222"
    if (boxValue(O1) == i3) return "FAIL 232"
    if (O1 != boxValue(O1)) return "FAIL 242"
    if (O1 == boxValue(O2)) return "FAIL 252"
    if (O1 == boxValue(i3)) return "FAIL 262"
    if (boxValue(O1) != boxValue(O1)) return "FAIL 272"
    if (boxValue(O1) == boxValue(O2)) return "FAIL 282"
    if (boxValue(O1) == boxValue(i3)) return "FAIL 292"

    if (O1 == coerceToI2(O2)) return "FAIL 213"
    if (O1 == coerceToI2(i3)) return "FAIL 223"

    if (coerceToI1(O1) != O1) return "FAIL 214"
    if (coerceToI1(O1) == O2) return "FAIL 224"
    if (coerceToI1(O1) == i3) return "FAIL 234"
    if (O1 != coerceToI1(O1)) return "FAIL 244"
    if (O1 == coerceToI1(O2)) return "FAIL 254"
    if (O1 == coerceToI1(i3)) return "FAIL 264"
    if (coerceToI1(O1) != coerceToI1(O1)) return "FAIL 274"
    if (coerceToI1(O1) == coerceToI1(O2)) return "FAIL 284"
    if (coerceToI1(O1) == coerceToI1(i3)) return "FAIL 294"

    if (coerceToAny(O1) != O1) return "FAIL 215"
    if (coerceToAny(O1) == O2) return "FAIL 225"
    if (coerceToAny(O1) == i3) return "FAIL 235"
    if (O1 != coerceToAny(O1)) return "FAIL 245"
    if (O1 == coerceToAny(O2)) return "FAIL 255"
    if (O1 == coerceToAny(i3)) return "FAIL 265"
    if (coerceToAny(O1) != coerceToAny(O1)) return "FAIL 275"
    if (coerceToAny(O1) == coerceToAny(O2)) return "FAIL 285"
    if (coerceToAny(O1) == coerceToAny(i3)) return "FAIL 295"

    if (O2 == O1) return "FAIL 311"
    if (O2 != O2) return "FAIL 321"
    if (O2 == i3) return "FAIL 331"

    if (boxValue(O2) == O1) return "FAIL 312"
    if (boxValue(O2) != O2) return "FAIL 322"
    if (boxValue(O2) == i3) return "FAIL 332"
    if (O2 == boxValue(O1)) return "FAIL 342"
    if (O2 != boxValue(O2)) return "FAIL 352"
    if (O2 == boxValue(i3)) return "FAIL 362"
    if (boxValue(O2) == boxValue(O1)) return "FAIL 372"
    if (boxValue(O2) != boxValue(O2)) return "FAIL 382"
    if (boxValue(O2) == boxValue(i3)) return "FAIL 392"

    if (coerceToI2(O2) == O1) return "FAIL 313"
    if (coerceToI2(O2) != O2) return "FAIL 323"
    if (coerceToI2(O2) == i3) return "FAIL 333"
    if (O2 != coerceToI2(O2)) return "FAIL 343"
    if (O2 == coerceToI2(i3)) return "FAIL 353"
    if (coerceToI2(O2) != coerceToI2(O2)) return "FAIL 363"
    if (coerceToI2(O2) == coerceToI2(i3)) return "FAIL 373"

    if (coerceToI1(O2) == O1) return "FAIL 314"
    if (coerceToI1(O2) != O2) return "FAIL 324"
    if (coerceToI1(O2) == i3) return "FAIL 334"
    if (O2 == coerceToI1(O1)) return "FAIL 344"
    if (O2 != coerceToI1(O2)) return "FAIL 354"
    if (O2 == coerceToI1(i3)) return "FAIL 364"
    if (coerceToI1(O2) == coerceToI1(O1)) return "FAIL 374"
    if (coerceToI1(O2) != coerceToI1(O2)) return "FAIL 384"
    if (coerceToI1(O2) == coerceToI1(i3)) return "FAIL 394"

    if (coerceToAny(O2) == O1) return "FAIL 315"
    if (coerceToAny(O2) != O2) return "FAIL 325"
    if (coerceToAny(O2) == i3) return "FAIL 335"
    if (O2 == coerceToAny(O1)) return "FAIL 345"
    if (O2 != coerceToAny(O2)) return "FAIL 355"
    if (O2 == coerceToAny(i3)) return "FAIL 365"
    if (coerceToAny(O2) == coerceToAny(O1)) return "FAIL 375"
    if (coerceToAny(O2) != coerceToAny(O2)) return "FAIL 385"
    if (coerceToAny(O2) == coerceToAny(i3)) return "FAIL 395"

    return "OK"
}
