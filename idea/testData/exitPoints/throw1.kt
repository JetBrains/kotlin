fun f(a: Int): Int {
    if (a < 5) {
        return 1
    }
    else {
        <caret>throw Error()
    }
}

//HIGHLIGHTED: return 1
//HIGHLIGHTED: throw Error()