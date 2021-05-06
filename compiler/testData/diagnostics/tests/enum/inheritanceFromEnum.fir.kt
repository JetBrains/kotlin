// FILE: MyJavaEnum.java
public enum MyJavaEnum {}

// FILE: test.kt
open enum class MyEnum() {
    A()
}

enum class MyEnum2() {}

class MyClass(): <!FINAL_SUPERTYPE, INVISIBLE_REFERENCE!>MyEnum2<!>() {}

class MyClass2(): <!FINAL_SUPERTYPE, UNRESOLVED_REFERENCE!>MyJavaEnum<!>() {}
