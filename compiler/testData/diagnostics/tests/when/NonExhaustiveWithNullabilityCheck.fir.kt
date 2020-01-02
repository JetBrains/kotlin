// KT-7857: when exhaustiveness does not take previous nullability checks into account
enum class X { A, B }
fun foo(arg: X?): Int {
    if (arg != null) {
        return when (arg) {
            X.B -> 2
        }
    } 
    else {
        return 0
    }
}