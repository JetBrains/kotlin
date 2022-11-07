// FIR_IDENTICAL
// WITH_STDLIB
// !DIAGNOSTICS: -EXTENSION_SHADOWED_BY_MEMBER
// LANGUAGE: +CustomBoxingInInlineClasses

@JvmInline
value class IC1<T>(val x: T)

@JvmInline
value class IC2(val x: Int)

inline fun <reified T> bar() = createInlineClassInstance<T>(5)

fun foo() {
    createInlineClassInstance<String>("abacaba")
    createInlineClassInstance<IC1<Int>>(3)
    bar<String>()
    bar<IC2>()
    bar<IC1<Int>>()
    createInlineClassInstance<IC2>(5.3)
    createInlineClassInstance<IC2>(1)
}