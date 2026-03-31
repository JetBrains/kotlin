class TopLevelClass {
    val memberProp: Int = 42

    fun memberFun() {}

    init {
        val x = 1
    }

    class Nested

    class Middle {
        val deepProp: Int = 42

        fun deepFun() {}
    }
}

fun topLevelFun() {}

val topLevelProp: Int = 42

typealias MyAlias = String
