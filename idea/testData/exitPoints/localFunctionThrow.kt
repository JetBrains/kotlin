fun f(a: Int): Int {
    fun localFun() {
        if (a > 5) {
            return
        }
        <caret>throw Error()
    }

    if (a < 5) {
        return 1
    }
    else {
        throw Exception()
    }
}

//HIGHLIGHTED: return
//HIGHLIGHTED: throw Error()
