// LANGUAGE: +InlineClasses

inline class Foo(val x: Int) {
    companion object {
        fun funInCompanion() {}

        private const val constValInCompanion = 1
    }

    fun inInlineClass() {}
}