// JAVAC_EXPECTED_FILE
class TestIface(r : Runnable) : Runnable by r {}

class TestObject(o : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Object<!>) : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN, DELEGATION_NOT_TO_INTERFACE!>Object<!> by o {}