// JAVAC_EXPECTED_FILE
class TestIface(r : Runnable) : Runnable by r {}

class TestObject(o : Object) : <!SUPERTYPE_NOT_INITIALIZED!>Object<!> by o {}
