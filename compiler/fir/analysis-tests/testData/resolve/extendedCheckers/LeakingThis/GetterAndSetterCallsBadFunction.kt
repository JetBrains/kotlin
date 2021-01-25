// WITH_RUNTIME

class GetterAndSetterCallsBadFunction {
    val s: String
    val property: Int
        get() {
            <!LEAKING_THIS!>s<!>.length
            return 1
        }

    init {
        print(property)
        s = ""
    }
}
