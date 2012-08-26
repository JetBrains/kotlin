class TestIface(r : Runnable) : Runnable by r {}

class TestObject(o : Object) : <!DELEGATION_NOT_TO_TRAIT!>Object<!> by o {}