class B {
    fun bar() {}

    val foo = 1

    object Nested {
        fun member() {

        }

        init {
            member()
        }

        val member: String
            get() = "str"
    }
}

fun topLevel() {

}

var topLevelVar = 1

topLevel()

123
