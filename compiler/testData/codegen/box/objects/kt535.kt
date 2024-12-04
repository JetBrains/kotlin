class Identifier<T>(t : T?, myHasDollar : Boolean) {
    private val myT : T?

    public fun getName() : T? { return myT }

    companion object {
        open public fun <T> init(name : T?) : Identifier<T> {
            val id = Identifier<T>(name, false)
            return id
        }
    }
    init {
        myT = t
    }
}

fun box() : String {
    var i3 : Identifier<String?>? = Identifier.init<String?>("OK")
    return i3?.getName() ?: "FAIL"
}
