// IS_APPLICABLE: false

class A(val x: String) {

    class C {<caret>}

    constructor(x: String, y: Int) : this(x) {
    }
}