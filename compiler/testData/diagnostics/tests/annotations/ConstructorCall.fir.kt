annotation class Ann
annotation class Ann1(val a: Int)
annotation class Ann2(val a: Ann1)

annotation class Ann3(val a: Ann1 = Ann1(1))

annotation class Ann4(val value: String)

@Ann2(Ann1(1)) val a = 1

@Ann2(a = Ann1(1)) val c = 2

@Ann4("a") class MyClass

fun foo() {
    Ann()
    val a = Ann()

    Ann1(<!NO_VALUE_FOR_PARAMETER!>)<!>
    Ann1(1)
    bar(Ann())
    bar(a = Ann())

    val ann = javaClass<MyClass>().getAnnotation(javaClass<Ann4>())
    ann!!.value()
}

fun bar(a: Ann = Ann()) {
    if (<!USELESS_IS_CHECK!>a is Ann<!>) {}
}

operator fun String.invoke() {}

// from stdlib
fun <T> javaClass() : Class<T> = null as Class<T>
