package mult_constructors_3_bug

public open class Identifier() {
    private var myNullable : Boolean = true
    default object {
        open public fun init(isNullable : Boolean) : Identifier {
            val __ = Identifier()
            __.myNullable = isNullable
            return __
        }
    }
}

fun box() : String {
    Identifier.init(true)
    return "OK"
}
