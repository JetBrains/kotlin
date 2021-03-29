// "Remove unnecessary non-null assertion (!!)" "true"
fun test(value : String) {
    value!!<caret>.equals("test")
}

/* IGNORE_FIR */