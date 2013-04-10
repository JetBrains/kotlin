fun foo(x: Int?): Boolean {
    val result = x!! == 0 && x : Int == 0
    x : Int
    return result
}
