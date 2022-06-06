import kotlin.reflect.KProperty

val a: Any
    field = "Test"

val b: Any
    private field = "Test"

val c: Any
    <!WRONG_MODIFIER_TARGET!>protected<!> field = "Test"

val d: Any
    internal field = "Test"

val e: Any
    <!WRONG_MODIFIER_TARGET!>public<!> field = "Test"

fun test() {
    val a2 = a#field
    val b2 = b#field
    val c2 = <!INVISIBLE_REFERENCE!>c#field<!>
    val d2 = d#field
    val e2 = e#field
}

internal val g: Any
    private field = "Test"

private val h: Any
    <!FIELD_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>internal<!> field = "Test"

private val i: Any
    private field = "Test"

internal val j: Any
    internal field = "Test"

public val k: Any
    private field = "Test"

public val l: Any
    internal field = "Test"
