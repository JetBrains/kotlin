fun test() {
    <caret>val x = 0
}

inline fun call(block: (Int) -> Int) {
    System.out.println(block(5))
}