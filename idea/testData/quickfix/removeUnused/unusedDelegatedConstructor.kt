// "Safe delete constructor" "false"
// ACTION: Convert to primary constructor
// ACTION: Make internal
// ACTION: Make private

class CtorUsedByOtherCtor {
    <caret>constructor()

    constructor(p: String): this()
}
