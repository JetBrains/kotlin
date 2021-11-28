fun ff(): Int {
    var i = 1
    { <!NAME_SHADOWING!>i<!>: Int -> i }
    return i
}
