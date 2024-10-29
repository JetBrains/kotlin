class OriginalClass {
    constructor() {}
}

typealias TypeAlias = OriginalClass

fun x() {
    val a = ::<caret>TypeAlias
}
