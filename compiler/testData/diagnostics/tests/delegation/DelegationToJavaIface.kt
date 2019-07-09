// JAVAC_EXPECTED_FILE
class TestIface(r : Runnable) : Runnable by r {}

class TestObject(o : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Object<!>) : <!DELEGATION_NOT_TO_INTERFACE, PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Object<!> by o {}