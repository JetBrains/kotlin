// IS_APPLICABLE: false
annotation class Inject
open class Test<caret> private constructor() {
    private @Inject constructor(i: Int) : this() {
    }
}