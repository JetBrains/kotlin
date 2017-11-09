fun log(s: String) {
}

class A {
    var x: String
        get() {
            log(field)
            return field
        }

    <caret>constructor(x: String) {
        this.x = x
    }
}
