// "Change the signature of function literal" "true"
// DISABLE-ERRORS

fun f(x: Int, y: Int, z : (Int, Int?, Any) -> Int) {
    f(1, 2, {(i: Int,
              i1: Int?,
              any: Any) -> x});
}
