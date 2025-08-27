// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

data class MyData(val value: String)

@JvmInline
value class MyValue(val value: String)

class MyClass(val value: String)

class ExplicitAnyOverride() {
    override fun hashCode(): Int = 0
    override fun toString(): String = ""
}

fun data() {
    <!RETURN_VALUE_NOT_USED!>MyData("")<!>
    val test = MyData("")
    <!RETURN_VALUE_NOT_USED!>test.value<!>
    <!RETURN_VALUE_NOT_USED!>test.toString()<!>
    <!RETURN_VALUE_NOT_USED!>test.equals(MyData(""))<!>
    <!RETURN_VALUE_NOT_USED!>test.hashCode()<!>
    <!RETURN_VALUE_NOT_USED!>test.component1()<!>
}

fun value() {
    <!RETURN_VALUE_NOT_USED!>MyValue("")<!>
    val test = MyValue("")
    <!RETURN_VALUE_NOT_USED!>test.value<!>
    <!RETURN_VALUE_NOT_USED!>test.toString()<!>
    <!RETURN_VALUE_NOT_USED!>test.hashCode()<!>
    <!RETURN_VALUE_NOT_USED!>test.equals(MyValue(""))<!>
}

fun clazz() {
    <!RETURN_VALUE_NOT_USED!>MyClass("")<!>
    val test = MyClass("")
    <!RETURN_VALUE_NOT_USED!>test.value<!>
    <!RETURN_VALUE_NOT_USED!>test.toString()<!>
    <!RETURN_VALUE_NOT_USED!>test.hashCode()<!>
    <!RETURN_VALUE_NOT_USED!>test.equals(MyClass(""))<!>
}

fun explicit() {
    <!RETURN_VALUE_NOT_USED!>ExplicitAnyOverride()<!>
    val test = ExplicitAnyOverride()
    <!RETURN_VALUE_NOT_USED!>test.toString()<!>
    <!RETURN_VALUE_NOT_USED!>test.hashCode()<!>
    <!RETURN_VALUE_NOT_USED!>test.equals(ExplicitAnyOverride())<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, localProperty, primaryConstructor,
propertyDeclaration, stringLiteral, value */
