// This test checks that annotations on extension function types are preserved. See the corresponding .txt file

@Target(AnnotationTarget.TYPE)
annotation class ann

interface Some {
    fun f1(): String.() -> Int
    fun f2(): @Extension String.() -> Int
    fun f3(): @ann String.() -> Int
    fun f4(): @Extension @ann String.() -> Int
}
