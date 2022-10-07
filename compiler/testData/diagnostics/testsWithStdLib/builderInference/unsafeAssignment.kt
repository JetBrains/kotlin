class Foo<T : Any> {
    fun doSmthng(arg: T) {}
    var a: T? = null
}

fun <T : Any> myBuilder(block: Foo<T>.() -> Unit) : Foo<T> = Foo<T>().apply(block)

fun main() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>myBuilder<!> {
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!> = "some string"
    }

    val x = 57
    val value = myBuilder {
        doSmthng("one ")
        a = 57
        a = x
    }
    println(value.a?.count { it in 'l' .. 'q' })
}
