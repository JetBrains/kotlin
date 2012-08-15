open class Test1(r : Runnable) : Runnable by r {}

open class Test2(o : Object) : <!DELEGATION_NOT_TO_TRAIT!>Object<!> by o {}