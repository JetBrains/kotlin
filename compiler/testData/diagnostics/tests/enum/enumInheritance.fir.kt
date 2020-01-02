// FILE: test.kt
enum class MyEnum(): MyClass() {}
enum class MyEnum2(): MyTrait {}
enum class MyEnum3(): <!INAPPLICABLE_CANDIDATE!>MyEnumBase<!>() {}

open class MyClass() {}

enum class MyEnumBase() {}

interface MyTrait {}

