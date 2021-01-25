// WITH_RUNTIME

class GetterAndSetterCallsBadFunction {
    val s: String
    val property: Int
        get() {
            <!MAY_BE_NOT_INITIALIZED!>s<!>.length
            return 1
        }

    init {
        print(property)
        s = ""
    }
}
