// IGNORE_BACKEND: WASM
package test

private val prop = "O"

private fun test() = "K"

fun box(): String {
    return {
        prop + test()
    }()
}