public fun box() : String {
    var i : Int?
    i = 10
    // We have "double" smart cast here:
    // first on i and second on i++
    // Back-end should NOT think that both i and j are Int
    val j: Int = i++

    return if (j == 10 && 11 == i) "OK" else "fail j = $j i = $i"
}
