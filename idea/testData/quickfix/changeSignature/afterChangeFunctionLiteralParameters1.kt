// "Change the signature of function literal" "true"
// DISABLE-ERRORS

fun f(x: Int, y: Int, z : () -> Int) {
    f(1, 2, {() -> x});
}
