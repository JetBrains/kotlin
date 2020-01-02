class Another {
    @Deprecated("Object")
    companion object {
        fun use() {}
        const val USE = 42
    }
}

fun first() {
    Another.use()
    Another.Companion.USE
    Another.USE
}

fun useCompanion() {
    val d = Another
    val x = Another.Companion
    Another.Companion.use()
    Another.use()
}

@Deprecated("Some")
class Some {
    companion object {
        fun use() {}
    }
}

fun some() {
    Some.use()
    Some.Companion.use()
}
