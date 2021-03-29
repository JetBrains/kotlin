// "Add non-null asserted (!!) call" "true"
fun foo(a: Int?) {
    a<caret>.plus(1)
}

/* IGNORE_FIR */