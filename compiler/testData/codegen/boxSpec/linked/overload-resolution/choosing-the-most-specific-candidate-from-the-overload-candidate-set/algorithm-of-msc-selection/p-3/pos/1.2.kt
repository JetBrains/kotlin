// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-435
 * MAIN LINK: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * NUMBER: 2
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
class Case (val y: Int, val x: Number)  {

    constructor(vararg x: Int) : this(1, 1 as Number){
        flag = true
    }
}
