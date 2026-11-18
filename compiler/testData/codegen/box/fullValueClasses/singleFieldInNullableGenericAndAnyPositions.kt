// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// Checks that instances of a single-field full value class and of an inline value class survive passing through
// nullable, generic and `Any` positions, where an inline class instance has to be boxed.

value class Full(val x: Int)

inline value class Inline(val x: Int)

interface Named {
    val name: String
}

value class FullNamed(override val name: String) : Named

inline value class InlineNamed(override val name: String) : Named

fun <T> id(value: T): T = value

fun <T : Any> idNotNull(value: T): T = value

fun anyOf(value: Any): Any = value

fun nullableAnyOf(value: Any?): Any? = value

fun nullableFull(value: Full, isNull: Boolean): Full? = if (isNull) null else value

fun nullableInline(value: Inline, isNull: Boolean): Inline? = if (isNull) null else value

class Box<T>(val value: T)

fun describe(value: Any?): String = when (value) {
    null -> "null"
    is Full -> "Full(${value.x})"
    is Inline -> "Inline(${value.x})"
    is Named -> "Named(${value.name})"
    else -> "other"
}

fun testNullable(): String {
    val full: Full? = nullableFull(Full(1), isNull = false)
    if (full == null || full.x != 1) return "Fail nullable Full"
    if (nullableFull(Full(1), isNull = true) != null) return "Fail null Full"
    if (full != Full(1) || full == Full(2)) return "Fail nullable Full equality"
    if (nullableFull(Full(1), isNull = true)?.x != null) return "Fail safe call on null Full"

    val inline: Inline? = nullableInline(Inline(1), isNull = false)
    if (inline == null || inline.x != 1) return "Fail nullable Inline"
    if (nullableInline(Inline(1), isNull = true) != null) return "Fail null Inline"
    if (inline != Inline(1) || inline == Inline(2)) return "Fail nullable Inline equality"
    if (nullableInline(Inline(1), isNull = true)?.x != null) return "Fail safe call on null Inline"

    return "OK"
}

fun testGeneric(): String {
    if (id(Full(2)).x != 2) return "Fail generic Full"
    if (idNotNull(Full(2)) != Full(2)) return "Fail generic not-null Full"
    if (id<Full?>(null) != null) return "Fail generic null Full"
    if (Box(Full(3)).value.x != 3) return "Fail Full in generic class"
    val fulls = listOf(Full(1), Full(2), Full(1))
    if (fulls.map { it.x } != listOf(1, 2, 1)) return "Fail list of Full"
    if (fulls.toSet().size != 2) return "Fail set of Full"
    if (mapOf(Full(1) to "one")[Full(1)] != "one") return "Fail Full as map key"
    if (arrayOf(Full(4), Full(5))[1].x != 5) return "Fail array of Full"
    if (run { Full(6) }.x != 6) return "Fail Full from lambda"

    if (id(Inline(2)).x != 2) return "Fail generic Inline"
    if (idNotNull(Inline(2)) != Inline(2)) return "Fail generic not-null Inline"
    if (id<Inline?>(null) != null) return "Fail generic null Inline"
    if (Box(Inline(3)).value.x != 3) return "Fail Inline in generic class"
    val inlines = listOf(Inline(1), Inline(2), Inline(1))
    if (inlines.map { it.x } != listOf(1, 2, 1)) return "Fail list of Inline"
    if (inlines.toSet().size != 2) return "Fail set of Inline"
    if (mapOf(Inline(1) to "one")[Inline(1)] != "one") return "Fail Inline as map key"
    if (arrayOf(Inline(4), Inline(5))[1].x != 5) return "Fail array of Inline"
    if (run { Inline(6) }.x != 6) return "Fail Inline from lambda"

    return "OK"
}

fun testAny(): String {
    val full: Any = anyOf(Full(7))
    if (full !is Full || full.x != 7) return "Fail Full as Any"
    if (full is Inline) return "Fail Full is not Inline"
    if ((full as Full).x != 7) return "Fail cast to Full"
    if ((full as? Inline) != null) return "Fail safe cast of Full to Inline"
    if (full != Full(7) || full == Full(8)) return "Fail Full equality through Any"
    if (full.hashCode() != Full(7).hashCode()) return "Fail Full hashCode through Any"
    if (full.toString() != "Full(x=7)") return "Fail Full toString through Any: $full"
    if (nullableAnyOf(null) is Full) return "Fail null is not Full"

    val inline: Any = anyOf(Inline(7))
    if (inline !is Inline || inline.x != 7) return "Fail Inline as Any"
    if (inline is Full) return "Fail Inline is not Full"
    if ((inline as Inline).x != 7) return "Fail cast to Inline"
    if ((inline as? Full) != null) return "Fail safe cast of Inline to Full"
    if (inline != Inline(7) || inline == Inline(8)) return "Fail Inline equality through Any"
    if (inline.hashCode() != Inline(7).hashCode()) return "Fail Inline hashCode through Any"
    if (inline.toString() != "Inline(x=7)") return "Fail Inline toString through Any: $inline"
    if (nullableAnyOf(null) is Inline) return "Fail null is not Inline"

    if (full == inline) return "Fail Full is not equal to Inline"

    val described = listOf<Any?>(Full(1), Inline(2), FullNamed("a"), InlineNamed("b"), null, 3).map(::describe)
    if (described != listOf("Full(1)", "Inline(2)", "Named(a)", "Named(b)", "null", "other")) return "Fail describe: $described"

    return "OK"
}

fun testSupertype(): String {
    val named: List<Named> = listOf(FullNamed("a"), InlineNamed("b"))
    if (named.map { it.name } != listOf("a", "b")) return "Fail names"
    if (named[0] !is FullNamed || named[1] !is InlineNamed) return "Fail instance checks through supertype"
    if (named[0] != FullNamed("a") || named[1] != InlineNamed("b")) return "Fail equality through supertype"
    return "OK"
}

fun box(): String {
    testNullable().let { if (it != "OK") return it }
    testGeneric().let { if (it != "OK") return it }
    testAny().let { if (it != "OK") return it }
    testSupertype().let { if (it != "OK") return it }
    return "OK"
}
