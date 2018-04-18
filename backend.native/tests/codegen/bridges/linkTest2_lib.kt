sealed class Tag {
    abstract fun value(): Any
}

sealed class TagBoolean : Tag() {
    abstract override fun value(): Boolean

    object True : TagBoolean() {
        override fun value() = true
    }

    object False : TagBoolean() {
        override fun value() = false
    }
}