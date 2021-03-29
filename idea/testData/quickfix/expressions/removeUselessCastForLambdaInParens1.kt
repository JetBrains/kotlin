// "Remove useless cast" "true"
fun test() {
    ({ "" } as<caret> () -> String)
}
/* IGNORE_FIR */