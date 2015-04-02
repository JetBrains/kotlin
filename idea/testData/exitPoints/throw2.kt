fun f(a: Int): Int {
    if (a < 5) {
        <caret>return 1
    }
    else {
        throw Error()
    }
}

//HIGHLIGHTED: return 1
//HIGHLIGHTED: throw Error()