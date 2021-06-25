// JAVAC_EXPECTED_FILE
class TestIface(r : Runnable) : Runnable by r {}

class TestObject(o : Object) : <!DELEGATION_NOT_TO_INTERFACE, SUPERTYPE_NOT_INITIALIZED!>Object<!> by o {}
