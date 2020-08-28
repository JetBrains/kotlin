// FIR_COMPARISON
fun foo(): String? = null

fun bar() {
    val f = foo() ?: return
    f.<caret>
}

// EXIST: length
// EXIST: hashCode
