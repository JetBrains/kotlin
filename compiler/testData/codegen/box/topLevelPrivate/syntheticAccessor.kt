// IGNORE_BACKEND_FIR: JVM_IR
package test

private val prop = "O"

private fun test() = "K"

fun box(): String {
    return {
        prop + test()
    }()
}