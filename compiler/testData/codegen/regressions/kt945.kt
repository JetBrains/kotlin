fun box() : String {
    val data = Array<Array<Boolean>>(3) { Array<Boolean>(4, {false}) }
    for(d in data) {
        if(d.size != 4) return "fail"
        System.out?.println(d)
    }

    return "OK"
}
