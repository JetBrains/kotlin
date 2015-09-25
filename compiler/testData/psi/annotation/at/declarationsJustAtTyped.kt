private @ @[Ann1(1)] @Ann3("2") class A(
        @ private val x: Int,
        @ private var y: Int,
        @ open z: Int
) {
    @ fun foo() {
        @ class LocalClass

        print(1)

        @

        @[inline2] private
        fun inlineLocal() {}

        @[Ann]
        private
        @
        @Volatile var x = 1

        foo(fun(@ @ann(1) x: Int) {})

        for (@ x in 1..100) {}
    }

    val x: Int
        @ private @ open get() = 1
}
