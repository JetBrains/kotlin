// FIR_COMPARISON
fun Some.simpleKotlinExtension() {
}

class Some() {
}

fun test() {
    val s = Some()
    s.<caret>
}

// EXIST: simpleKotlinExtension