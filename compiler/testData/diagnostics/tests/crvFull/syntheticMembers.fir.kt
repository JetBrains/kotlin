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
    <!RETURN_VALUE_NOT_USED!>MyData<!>("")
    val test = MyData("")
    test.<!RETURN_VALUE_NOT_USED!>value<!>
    test.<!RETURN_VALUE_NOT_USED!>toString<!>()
    test.<!RETURN_VALUE_NOT_USED!>equals<!>(MyData(""))
    test.<!RETURN_VALUE_NOT_USED!>hashCode<!>()
    test.<!RETURN_VALUE_NOT_USED!>component1<!>()
}

fun value() {
    <!RETURN_VALUE_NOT_USED!>MyValue<!>("")
    val test = MyValue("")
    test.<!RETURN_VALUE_NOT_USED!>value<!>
    test.<!RETURN_VALUE_NOT_USED!>toString<!>()
    test.<!RETURN_VALUE_NOT_USED!>hashCode<!>()
    test.<!RETURN_VALUE_NOT_USED!>equals<!>(MyValue(""))
}

fun clazz() {
    <!RETURN_VALUE_NOT_USED!>MyClass<!>("")
    val test = MyClass("")
    test.<!RETURN_VALUE_NOT_USED!>value<!>
    test.<!RETURN_VALUE_NOT_USED!>toString<!>()
    test.<!RETURN_VALUE_NOT_USED!>hashCode<!>()
    test.<!RETURN_VALUE_NOT_USED!>equals<!>(MyClass(""))
}

fun explicit() {
    <!RETURN_VALUE_NOT_USED!>ExplicitAnyOverride<!>()
    val test = ExplicitAnyOverride()
    test.<!RETURN_VALUE_NOT_USED!>toString<!>()
    test.<!RETURN_VALUE_NOT_USED!>hashCode<!>()
    test.<!RETURN_VALUE_NOT_USED!>equals<!>(ExplicitAnyOverride())
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, localProperty, primaryConstructor,
propertyDeclaration, stringLiteral, value */
