class Z(s: (Int) -> Int) {

}

public inline fun test(s : (Int) -> Int) {
    Z(s)
}