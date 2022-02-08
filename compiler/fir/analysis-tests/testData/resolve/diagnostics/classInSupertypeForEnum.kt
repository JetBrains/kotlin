open class A

interface C

enum class B : C, <!CLASS_IN_SUPERTYPE_FOR_ENUM!>A<!>(), <!MANY_CLASSES_IN_SUPERTYPE_LIST!>Any<!>()
