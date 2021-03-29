// "Remove 'operator' modifier" "true"
// COMPILER_ARGUMENTS: -XXLanguage:+ProhibitOperatorMod

object A {
    operator<caret> fun mod(x: Int) {}
}
/* IGNORE_FIR */