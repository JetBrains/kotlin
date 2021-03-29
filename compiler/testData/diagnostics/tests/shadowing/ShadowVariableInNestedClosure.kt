fun f(): Int {
    var i = 17
    { var <!NAME_SHADOWING!>i<!> = 18 }
    return i
}
