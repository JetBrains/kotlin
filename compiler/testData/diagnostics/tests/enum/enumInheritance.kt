// FILE: test.kt
enum class MyEnum(): <!CLASS_IN_SUPERTYPE_FOR_ENUM!>MyClass<!>() {}
enum class MyEnum2(): MyTrait {}
enum class MyEnum3(): <!INVISIBLE_MEMBER, CLASS_IN_SUPERTYPE_FOR_ENUM, FINAL_SUPERTYPE!>MyEnumBase<!>() {}

open class MyClass() {}

enum class MyEnumBase() {}

interface MyTrait {}

