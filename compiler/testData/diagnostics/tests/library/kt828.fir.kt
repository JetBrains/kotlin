fun test() {
    var res : Boolean = true
    res = (res and false)
    res = (res or false)
    res = (res xor false)
    res = (true and false)
    res = (true or false)
    res = (true xor false)
    res = (!true)
    res = (true && false)
    res = (true || false)
}
