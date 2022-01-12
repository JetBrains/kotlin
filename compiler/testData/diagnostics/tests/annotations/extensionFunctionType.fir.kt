// WITH_STDLIB
// !LANGUAGE: +ForbidExtensionFunctionTypeOnNonFunctionTypes
// This test checks that annotations on extension function types are preserved. See the corresponding .txt file

@Target(AnnotationTarget.TYPE)
annotation class ann

interface Some {
    fun f1(): String.() -> Int
    fun f2(): @ExtensionFunctionType() (String.() -> Int)
    fun f3(): @ann String.() -> Int
    fun f4(): @ExtensionFunctionType @ann() (String.() -> Int)

    fun f5(): <!WRONG_EXTENSION_FUNCTION_TYPE!>@ExtensionFunctionType<!> () -> Int

    fun f6(x: <!WRONG_EXTENSION_FUNCTION_TYPE!>@ExtensionFunctionType<!> () -> Int) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(x <!UNRESOLVED_REFERENCE!>+<!> 2)
    }

    fun f7(x: <!WRONG_EXTENSION_FUNCTION_TYPE!>@ExtensionFunctionType<!> Function0<Int>)

    fun f8(x: <!WRONG_EXTENSION_FUNCTION_TYPE!>@ExtensionFunctionType<!> Int)
}
