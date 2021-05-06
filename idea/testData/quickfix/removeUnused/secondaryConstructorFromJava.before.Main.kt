// "Safe delete constructor" "false"
// ACTION: Convert to primary constructor
// ACTION: Make internal
// ACTION: Make private

class Ctor {
    <caret>constructor(p: Int)

    fun justCompare() {}
}
