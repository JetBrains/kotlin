open class A

class <caret>B: A {
    // INFO: {"checked": "true"}
    var n: Int

    constructor(a: Int) {
        n = a + 1
        n = a plus 1
    }
}