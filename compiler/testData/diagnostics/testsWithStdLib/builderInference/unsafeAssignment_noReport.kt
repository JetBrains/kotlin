// !LANGUAGE: +NoBuilderInferenceWithoutAnnotationRestriction
// FIR_DUMP

class Foo<T : Any> {
    fun doSmthng(arg: T) {}
    var a: T? = null
}

fun <T : Any> myBuilder(block: Foo<T>.() -> Unit) : Foo<T> = Foo<T>().apply(block)

fun main(arg: Any) {
    val x = 57
    val value = myBuilder {
        doSmthng("one ")
        a = 57
        a = x
        if (arg is String) {
            a = arg
        }
    }
    println(value.a?.count { it in 'l' .. 'q' })
}
