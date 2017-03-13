fun box() : String {
    val i : Int? = 0
    val j = i?.plus(3) //verify error
    return "OK"
}
