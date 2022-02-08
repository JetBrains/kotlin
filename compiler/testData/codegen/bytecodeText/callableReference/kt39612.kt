fun foo() {}

fun id(s: String) = s

fun test1() = id(::foo.name)

fun test2(name: String) = (if (name == ::foo.name) ::foo else ::id).annotations

// 0 getName
// 3 LDC "foo"