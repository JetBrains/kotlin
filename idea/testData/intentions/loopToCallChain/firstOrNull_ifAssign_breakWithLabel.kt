// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo() {
    MainLoop@
    for (i in 1..10) {
        var result: String? = null
        <caret>for (s in list()) {
            if (s.length > 0) {
                result = s
                break@MainLoop
            }
        }
    }
}

fun list(): List<String> = listOf()