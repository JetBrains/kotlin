// SKIP_TXT
// FIR_DUMP
// ISSUE: KT-38895, KT-50996, KT-51000

interface A
interface B

fun takeByte(x: Byte) {}

fun takeInt(x: Int) {}
fun takeNullableInt(x: Int) {}

fun takeLong(x: Long) {}
fun takeNullableLong(x: Long) {}

fun takeOverloaded(x: Int): A = null!! // (1)
fun takeOverloaded(x: Long): B = null!! // (2)

fun takeA(a: A) {}
fun takeB(b: B) {}

fun test_constants() {
    takeByte(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 + 1<!>) // error
    takeInt(1 + 1 + 1) // OK
    takeNullableInt(1 + 1 + 1) // OK
    takeLong(1 + 1 + 1) // will be OK with implicit widening conversion
    takeNullableLong(1 + 1 + 1) // will be OK with implicit widening conversion
    val x = takeOverloaded(<!INTEGER_OVERFLOW!>2147483648 - 1 + 1<!>) // now resolved to (1), will be resolved to (2)
    takeA(x)
    takeB(<!TYPE_MISMATCH!>x<!>)
}

val topLevelIntProperty: Int = 1 + 1 + 1
val topLevelLongProperty: Long = 1 + 1 + 1
val topLevelImplicitIntProperty = 1 + 1 + 1
val topLevelImplicitLongProperty = 3000000000 * 2 + 1

fun testTopLevelProperties() {
    // OK
    takeInt(topLevelIntProperty)
    takeLong(topLevelLongProperty)
    takeInt(topLevelImplicitIntProperty)
    takeLong(topLevelImplicitLongProperty)

    // no conversion for properties
    takeLong(<!TYPE_MISMATCH!>topLevelIntProperty<!>)
    takeLong(<!TYPE_MISMATCH!>topLevelImplicitIntProperty<!>)
}

fun testLocalProperties() {
    val localIntProperty: Int = 1 + 1
    val localLongProperty: Long = 1 + 1
    val localImplicitIntProperty = 1 + 1
    val localImplicitLongProperty = 3000000000 * 2

    // OK
    takeInt(localIntProperty)
    takeLong(localLongProperty)
    takeInt(localImplicitIntProperty)
    takeLong(localImplicitLongProperty)

    // no conversion for properties
    takeLong(<!TYPE_MISMATCH!>localIntProperty<!>)
    takeLong(<!TYPE_MISMATCH!>localImplicitIntProperty<!>)
}
