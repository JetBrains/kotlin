enum class Color(val rgb : Int) {
    RED(0xFF000) {
        override fun foo(): Int { return 1 }
    }
    GREEN(0x00FF00) {
        override fun foo(): Int { return 2 }
    }
    BLUE(0x0000FF) {
        override fun foo(): Int { return 3 }
    }
    
    abstract fun foo(): Int
}