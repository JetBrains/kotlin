// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Id(val id: String)

inline class Name(val name: String)

inline class Password(val password: String)

fun test(id: Id) {
    if (id.id != "OK") throw AssertionError()
}

fun test(id: Id?) {
    if (id != null) throw AssertionError()
}

fun test(name: Name) {
    if (name.name != "OK") throw AssertionError()
}

fun test(password: Password) {
    if (password.password != "OK") throw AssertionError()
}

// 1 public final static test-tmnojjU\(Ljava/lang/String;\)V
// 1 public final static test-hI9h0ro\(Ljava/lang/String;\)V
// 1 public final static test-75PUH38\(Ljava/lang/String;\)V
// 1 public final static test-3mN7H-Y\(Ljava/lang/String;\)V