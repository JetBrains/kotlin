inline fun <reified T : Any> foo(t: T): T {
    val klass = <!OTHER_ERROR!>T<!>::class.<!INAPPLICABLE_CANDIDATE!>java<!>
    return t
}