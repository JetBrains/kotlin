package kotlin.enums

enum class KtEnumDirection {
    NORTH
    SOUTH
    WEST
    EAST
}

enum class KtEnumColor(val rgb : Int) {
    RED : Color(0xFF0000)
    GREEN : Color(0x00FF00)
    BLUE : Color(0x0000FF)
}

//enum class KtEnumProtocolState {
//    WAITING {
//        override fun signal() = TALKING
//    }
//
//    TALKING {
//        override fun signal() = WAITING
//    }
//
//    abstract fun signal() : ProtocolState
//}

enum class KtEnumList<out T>(val size : Int) {
    Nil : List<Nothing>(0)
    Cons<T>(h : T, t : List<T>) : List<T>(t.size + 1)
}