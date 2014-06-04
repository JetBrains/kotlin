fun test1(): Int {
    return calc( {(l : Int) -> 2*l},  {(l : Int) -> 4*l})
}


fun box(): String {
    if (test1() != 110) return "test1: ${test1()}"

    return "OK"
}