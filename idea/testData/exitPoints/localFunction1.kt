fun f(a: Int): Int {
    fun localFun() {
        <caret>return
    }

    if (a < 5) {
        return 1
    }
    else {
        return 2
    }
}

//HIGHLIGHTED: return
