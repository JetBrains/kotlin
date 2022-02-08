// FIR_IDENTICAL
package f


//KT-3444 Front-end doesn't check if local function or function of anonymous class returns value

fun box(): Int {

    fun local(): Int {
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

    return local()
}

fun main() {
    box()
}

