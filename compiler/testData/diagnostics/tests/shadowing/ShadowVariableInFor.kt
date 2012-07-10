fun ff(): Int {
    var i = 1
    for (<!NAME_SHADOWING!>i<!> in 0..10) {
    }
    return i
}
