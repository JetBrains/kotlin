import BExtSpace.aaa

// "Import" "true"
// WITH_RUNTIME
// ERROR: Unresolved reference: aaa
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

fun test() {
    AAA().apply {
        sub {
            aaa<caret>()
        }
    }
}
/* IGNORE_FIR */