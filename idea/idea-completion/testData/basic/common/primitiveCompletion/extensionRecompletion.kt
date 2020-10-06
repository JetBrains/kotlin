// FIR_COMPARISON

class LocalClass

fun LocalClass.ext(action: () -> Unit) {}
fun LocalClass.extAnother(action: () -> Unit) {}

fun usage(l: LocalClass) {
    l.ext<caret> {}
}

// EXIST: ext
// EXIST: extAnother
