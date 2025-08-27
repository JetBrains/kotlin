// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// This file compiled NOT in must use mode, so only overrides of Any functions have to be reported

data class MyData(val value: String)

@JvmInline
value class MyValue(val value: String)

class MyClass(val value: String)

class ExplicitAnyOverride() {
    override fun hashCode(): Int = 0
    override fun toString(): String = ""
}

fun data() {
    MyData("")
    val test = MyData("")
    test.value
    <!RETURN_VALUE_NOT_USED!>test.toString()<!>
    <!RETURN_VALUE_NOT_USED!>test.equals(MyData(""))<!>
    <!RETURN_VALUE_NOT_USED!>test.hashCode()<!>
    test.component1()
}

fun value() {
    MyValue("")
    val test = MyValue("")
    test.value
    <!RETURN_VALUE_NOT_USED!>test.toString()<!>
    <!RETURN_VALUE_NOT_USED!>test.hashCode()<!>
    <!RETURN_VALUE_NOT_USED!>test.equals(MyValue(""))<!>
}

fun clazz() {
    MyClass("")
    val test = MyClass("")
    test.value
    <!RETURN_VALUE_NOT_USED!>test.toString()<!>
    <!RETURN_VALUE_NOT_USED!>test.hashCode()<!>
    <!RETURN_VALUE_NOT_USED!>test.equals(MyClass(""))<!>
}

fun explicit() {
    ExplicitAnyOverride()
    val test = ExplicitAnyOverride()
    <!RETURN_VALUE_NOT_USED!>test.toString()<!>
    <!RETURN_VALUE_NOT_USED!>test.hashCode()<!>
    <!RETURN_VALUE_NOT_USED!>test.equals(ExplicitAnyOverride())<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, localProperty, primaryConstructor,
propertyDeclaration, stringLiteral, value */
