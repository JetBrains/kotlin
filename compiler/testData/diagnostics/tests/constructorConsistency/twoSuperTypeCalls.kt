open class A(s: String)
open class B(): A("1")
class C(): A(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>100<!>), <!MANY_CLASSES_IN_SUPERTYPE_LIST!>B<!>()
