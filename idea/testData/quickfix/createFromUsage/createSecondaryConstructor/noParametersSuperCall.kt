// "Create secondary constructor" "true"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION
// ERROR: No value passed for parameter 'f'
open class Base(val f: Int)

class Creation: Base {
    constructor(f: Int): super(f)
}
val v = Creation(<caret>)