fun test(a: Int) {}

fun some() {
    tes<caret>
}

// EXIST: { lookupString:"test", itemText:"test(a: jet.Int)" }