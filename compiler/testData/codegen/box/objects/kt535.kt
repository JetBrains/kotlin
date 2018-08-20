// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

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
    var i3 : Identifier<String?>? = Identifier.init<String?>("name")
    System.out?.println(i3?.getName())
    return "OK"
}
