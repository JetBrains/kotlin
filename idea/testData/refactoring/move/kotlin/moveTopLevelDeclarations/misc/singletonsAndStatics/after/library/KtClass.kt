package library

class KtClass {
    class Inner {
        class object {
            fun foo(): Int = 1
        }
    }

    class object {
        fun foo(): Int = 1
    }
}

object KtObject {
    class Inner {
        class object {
            fun foo(): Int = 1
        }
    }

    fun foo(): Int = 1
}