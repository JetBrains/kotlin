package demo

public open class Identifier<T>(myName : T?, myHasDollar : Boolean) {
    private val myName : T?
    private var myHasDollar : Boolean
    private var myNullable : Boolean = true

    {
        $myName = myName
        $myHasDollar = myHasDollar
    }

    open public fun getName() : T? {
        return myName
    }

    default object {
        open public fun init<T>(name : T?) : Identifier<T> {
            val __ = Identifier<T>(name, false)
            return __
        }
    }
}

fun box() : String {
    var i3 : Identifier<*>? = Identifier.init<String?>("name")
    System.out?.println("Hello, " + i3?.getName())
    return "OK"
}
