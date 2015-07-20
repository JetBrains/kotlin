open class A

class <caret>B: A {
    // INFO: {"checked": "true"}
    val n: Int

    constructor(a: Int) {
        n = 1 + 2
    }

    constructor() {
        n = 1 + 2
    }
}