open class RecA<T>: <!CYCLIC_INHERITANCE_HIERARCHY!>RecB<T><!>()
open class RecB<T>: <!CYCLIC_INHERITANCE_HIERARCHY!>RecA<T><!>()
open class SelfR<T>: <!CYCLIC_INHERITANCE_HIERARCHY!>SelfR<T><!>()

fun test(f: SelfR<String>) = <!USELESS_IS_CHECK!>f is <!CANNOT_CHECK_FOR_ERASED!>RecA<String><!><!>
fun test(f: RecB<String>) = <!USELESS_IS_CHECK!>f is <!CANNOT_CHECK_FOR_ERASED!>RecA<String><!><!>
