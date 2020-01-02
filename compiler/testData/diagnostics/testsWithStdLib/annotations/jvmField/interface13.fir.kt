// !LANGUAGE: +JvmFieldInInterface

interface A {

    companion object {
        @JvmField
        val c = 3
    }
}


interface B {

    companion object {
        @JvmField
        val c = 3

        @JvmField
        val a = 3
    }
}

interface C {
    companion object {
        @JvmField
        val c = 3

        val a = 3
    }
}

interface D {
    companion object {
        @JvmField
        var c = 3
    }
}


interface E {
    companion object {
        @JvmField
        private val a = 3

        @JvmField
        internal val b = 3

        @JvmField
        protected val c = 3
    }
}


interface F {
    companion object {
        @JvmField
        open val a = 3
    }
}
