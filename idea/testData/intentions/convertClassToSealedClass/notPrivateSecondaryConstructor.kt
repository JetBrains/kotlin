// IS_APPLICABLE: false
open class Test<caret> private constructor() {
    constructor(i: Int) : this() {
    }
}