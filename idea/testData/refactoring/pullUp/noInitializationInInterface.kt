interface T

class <caret>A: T {
    // INFO: {"checked": "true"}
    val n: Int

    constructor(a: Int) {
        n = a + 1
    }
}