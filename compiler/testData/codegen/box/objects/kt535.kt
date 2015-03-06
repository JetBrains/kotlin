class Identifier<T>(t : T?, myHasDollar : Boolean) {
    private val myT : T?

    public fun getName() : T? { return myT }

    default object {
        open public fun init<T>(name : T?) : Identifier<T> {
            val __ = Identifier<T>(name, false)
            return __
        }
    }
    {
        $myT = t
    }
}

fun box() : String {
    var i3 : Identifier<String?>? = Identifier.init<String?>("name")
    System.out?.println(i3?.getName())
    return "OK"
}
