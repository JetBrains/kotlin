class Identifier() {
    private var myNullable : Boolean = false
        set(l : Boolean) {
            //do nothing
        }

    fun getValue() : Boolean {
        return myNullable
    }

    default object {
        fun init(isNullable : Boolean) : Identifier {
            val id = Identifier()
            id.myNullable = isNullable
            return id
        }
    }
}

fun box() : String {
    val id = Identifier.init(true)
    return if (id.getValue() == false) return "OK" else "fail"
}
