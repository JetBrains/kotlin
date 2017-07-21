// "Import" "true"
// WITH_RUNTIME
// ERROR: Unresolved reference: aaa

fun test() {
    AAA().apply {
        sub {
            aaa<caret>()
        }
    }
}