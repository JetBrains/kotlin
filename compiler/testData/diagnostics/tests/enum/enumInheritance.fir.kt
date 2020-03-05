// FILE: test.kt
enum class MyEnum(): MyClass() {}
enum class MyEnum2(): MyTrait {}
enum class MyEnum3(): MyEnumBase() {}

open class MyClass() {}

enum class MyEnumBase() {}

interface MyTrait {}

