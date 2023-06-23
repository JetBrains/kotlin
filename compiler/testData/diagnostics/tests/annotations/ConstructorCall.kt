// FIR_IDENTICAL
// !LANGUAGE: -InstantiationOfAnnotationClasses
annotation class Ann
annotation class Ann1(val a: Int)
annotation class Ann2(val a: Ann1)

annotation class Ann3(val a: Ann1 = Ann1(1))

annotation class Ann4(val value: String)

@Ann2(Ann1(1)) val a = 1

@Ann2(a = Ann1(1)) val c = 2

@Ann4("a") class MyClass

fun foo() {
    <!ANNOTATION_CLASS_CONSTRUCTOR_CALL!>Ann()<!>
    val a = <!ANNOTATION_CLASS_CONSTRUCTOR_CALL!>Ann()<!>

    <!ANNOTATION_CLASS_CONSTRUCTOR_CALL!>Ann1<!NO_VALUE_FOR_PARAMETER!>()<!><!>
    <!ANNOTATION_CLASS_CONSTRUCTOR_CALL!>Ann1(1)<!>
    bar(<!ANNOTATION_CLASS_CONSTRUCTOR_CALL!>Ann()<!>)
    bar(a = <!ANNOTATION_CLASS_CONSTRUCTOR_CALL!>Ann()<!>)

    val ann = javaClass<MyClass>().getAnnotation(javaClass<Ann4>())
    ann!!.value()
}

fun bar(a: Ann = <!ANNOTATION_CLASS_CONSTRUCTOR_CALL!>Ann()<!>) {
    if (<!USELESS_IS_CHECK!>a is Ann<!>) {}
}

operator fun String.invoke() {}

// from stdlib
fun <T> javaClass() : Class<T> = null <!CAST_NEVER_SUCCEEDS!>as<!> Class<T>
