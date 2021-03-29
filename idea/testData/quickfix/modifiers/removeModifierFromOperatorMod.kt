// "Remove 'operator' modifier" "true"

object A {
    operator<caret> fun mod(x: Int) {}
}
/* IGNORE_FIR */