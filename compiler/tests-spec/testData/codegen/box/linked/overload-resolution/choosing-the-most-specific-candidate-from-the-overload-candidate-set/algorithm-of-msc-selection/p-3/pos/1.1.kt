// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc%2B0.3-603
 * MAIN LINK: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: If both X_k and Y_k are built-in integer types, a type constraint Widen(X_k) <:Widen(Y_k) is built
 */

var flag  = false
fun box(): String {
    Case(1, 1)
    if (flag) {
        return "OK"
    }else
    return "NOK"
}
class Case {
    constructor(y: Int, x: Number){
        flag = false
    }
    constructor(vararg x: Int){
        flag = true
    }
}
