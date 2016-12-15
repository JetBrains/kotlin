fun xfoo(p: suspend () -> Unit) {}

fun test() {
    xf<caret>
}

// EXIST: { itemText:"xfoo", tailText:" {...} (p: suspend () -> Unit) (<root>)", typeText:"Unit" }
// EXIST: { itemText:"xfoo", tailText:"(p: suspend () -> Unit) (<root>)", typeText:"Unit" }
// NOTHING_ELSE