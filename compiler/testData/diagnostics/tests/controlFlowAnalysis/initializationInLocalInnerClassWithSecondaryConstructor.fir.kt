// ISSUE: KT-65299

val foo = object {
    inner class Inner {
        val field: Any

        constructor(field: Any) {
            this.field = field
        }

        val property get() = field
    }
}

val bar = object {
    inner class Inner {
        val field: Any

        constructor(field: Any) {
            this.field = field
        }

        fun function() = field
    }
}