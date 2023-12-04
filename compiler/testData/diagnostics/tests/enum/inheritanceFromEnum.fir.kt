// FILE: MyJavaEnum.java
public enum MyJavaEnum {}

// FILE: test.kt
<!WRONG_MODIFIER_TARGET!>open<!> enum class MyEnum() {
    A()
}

enum class MyEnum2() {}

class MyClass(): <!FINAL_SUPERTYPE, INVISIBLE_REFERENCE!>MyEnum2<!>() {}

class MyClass2(): <!FINAL_SUPERTYPE, UNRESOLVED_REFERENCE!>MyJavaEnum<!>() {}
