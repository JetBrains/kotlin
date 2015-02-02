class B<R> {
    constructor(x: String) {}
    constructor(x: R) {}
}

val y8: B<String> = <caret>B("")
