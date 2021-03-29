// "Remove useless cast" "true"
fun foo() {}

fun main() {
    foo();
    ({ "" } as<caret> () -> String)
}

/* IGNORE_FIR */