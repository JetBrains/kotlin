// "Remove unnecessary non-null assertion (!!)" "true"
fun test(value : Int) : Int {
    return value<caret>!!
}

/* IGNORE_FIR */