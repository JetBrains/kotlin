import kotlin.reflect.KClass

val javaClass: Class<String> = String::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
val kotlinClass: KClass<String> = String::class

fun foo() {
    val stringClass = String::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
    val arrayStringClass = Array<String>::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
}

