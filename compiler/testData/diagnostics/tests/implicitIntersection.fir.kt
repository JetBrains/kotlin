// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE
// See KT-10244: no intersection types in signatures

open class B
interface A
interface C

// Error!
fun foo(b: B) = if (b is A && b is C) b else null

// Ok: given explicitly
fun gav(b: B): A? = if (b is A && b is C) b else null

class My(b: B) {
    // Error!
    val x = if (b is A && b is C) b else null
    // Ok: given explicitly
    val y: C? = if (b is A && b is C) b else null
    // Error!
    fun foo(b: B) = if (b is A && b is C) b else null
}

fun bar(b: B): String {
    // Ok: local variable
    val tmp = if (b is A && b is C) b else null
    // Error: local function
    fun foo(b: B) = if (b is A && b is C) b else null
    return tmp.toString()
}