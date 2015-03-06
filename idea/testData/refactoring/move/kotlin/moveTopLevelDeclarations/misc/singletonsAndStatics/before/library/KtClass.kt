package library

class KtClass {
    class Inner {
        default object {
            fun foo(): Int = 1
        }
    }

    default object {
        fun foo(): Int = 1
    }
}

object KtObject {
    class Inner {
        default object {
            fun foo(): Int = 1
        }
    }

    fun foo(): Int = 1
}