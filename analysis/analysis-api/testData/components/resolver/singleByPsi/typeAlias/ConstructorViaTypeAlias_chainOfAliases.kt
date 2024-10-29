class OriginalClass {
    constructor() {}
}

typealias TypeAliasOne = OriginalClass

typealias TypeAliasTwo = TypeAliasOne

fun x() {
    val a = <caret>TypeAliasTwo()
}
