// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: common
expect class Typealiased

annotation class Ann(val p: kotlin.reflect.KClass<*>)

@Ann(Typealiased::class)
expect fun test()

@Ann(Array<Typealiased>::class)
expect fun testInArray()

// MODULE: main()()(common)
class TypealiasedImpl

actual typealias Typealiased = TypealiasedImpl

@Ann(Typealiased::class)
actual fun test() {}

@Ann(Array<Typealiased>::class)
actual fun testInArray() {}