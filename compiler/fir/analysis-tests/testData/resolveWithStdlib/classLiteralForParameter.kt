inline fun <reified T : Any> foo(t: T): T {
    val klass = T::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
    return t
}