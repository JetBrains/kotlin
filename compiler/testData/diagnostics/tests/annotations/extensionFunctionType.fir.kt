// This test checks that annotations on extension function types are preserved. See the corresponding .txt file

@Target(AnnotationTarget.TYPE)
annotation class ann

interface Some {
    fun f1(): String.() -> Int
    fun f2(): @ExtensionFunctionType() (String.() -> Int)
    fun f3(): @ann String.() -> Int
    fun f4(): @ExtensionFunctionType @ann() (String.() -> Int)
}
