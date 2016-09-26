// "Remove 'lateinit' modifier" "true"

class Foo {
    <caret>lateinit var bar: String

    constructor() {
        bar = ""
    }

    constructor(baz: Int) {
        bar = ""
    }
}