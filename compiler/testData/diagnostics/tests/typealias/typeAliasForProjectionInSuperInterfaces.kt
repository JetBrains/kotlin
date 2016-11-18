interface I<T>

typealias IStar = I<*>
typealias IIn = I<in Int>
typealias IOut = I<out Int>
typealias IT<T> = I<T>

class Test1 : <!EXPANDED_TYPE_CANNOT_BE_INHERITED!>IStar<!>
class Test2 : <!EXPANDED_TYPE_CANNOT_BE_INHERITED!>IIn<!>
class Test3 : <!EXPANDED_TYPE_CANNOT_BE_INHERITED!>IOut<!>
class Test4 : IT<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE!>*<!>>
class Test5 : IT<IT<*>>