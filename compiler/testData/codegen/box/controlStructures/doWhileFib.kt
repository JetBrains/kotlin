fun box(): String {
    var fx = 1
    var fy = 1
    
    do {
        var tmp = fy
        fy = fx + fy
        fx = tmp
    } while (fy < 100)
    
    return if (fy == 144) "OK" else "Fail $fx $fy"
}
