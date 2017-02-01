// IS_APPLICABLE: false

class My {
    val x: Int<caret>

    constructor(x: Int) {
        this.x = x
    }

    constructor() {
        x = 42
    }
}