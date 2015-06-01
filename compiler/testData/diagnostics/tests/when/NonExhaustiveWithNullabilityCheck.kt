// KT-7857: when exhaustiveness does not take previous nullability checks into account
enum class X { A, B }
fun foo(arg: X?): Int {
    if (arg != null) {
        return <!NO_ELSE_IN_WHEN!>when<!> (<!DEBUG_INFO_SMARTCAST!>arg<!>) {
            X.B -> 2
        }
    } 
    else {
        return 0
    }
}