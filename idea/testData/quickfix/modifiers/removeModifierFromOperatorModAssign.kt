// "Remove 'operator' modifier" "true"

object A {
    operator<caret> fun modAssign(x: Int) {}
}
/* IGNORE_FIR */