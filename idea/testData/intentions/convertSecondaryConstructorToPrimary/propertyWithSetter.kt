fun log(s: String) {
}

class A {
    var x: String
        set(value) {
            log(value)
            field = value
        }

    <caret>constructor(x: String) {
        this.x = x
    }
}