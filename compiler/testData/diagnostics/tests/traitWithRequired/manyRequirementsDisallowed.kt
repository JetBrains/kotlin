open class A
open class B

trait C : A, <!MANY_CLASSES_IN_SUPERTYPE_LIST!>B<!>
