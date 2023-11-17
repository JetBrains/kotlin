// KT-61929
// WITH_SDTLIB
// EXPECTED_REACHABLE_NODES: 1301
package foo

fun doSomething(lambda: () -> Unit) { lambda() }

class CompilerBug(result: String) {
    var result: String = "Failed"

    init {
        run {
            object {
                init {
                    doSomething { completed(result) }
                }
            }
        }
    }

    fun completed(value: String) {
        this.result = value
    }
}

fun box(): String {
    return CompilerBug("OK").result
}