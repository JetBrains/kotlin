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

// 1 public final static test-9zx0e0j9\(Ljava/lang/String;\)V
// 1 public final static test-79jv2l6i\(Ljava/lang/String;\)V
// 1 public final static test-d4pejdz3\(Ljava/lang/String;\)V
// 1 public final static test-c6sgoxk6\(Ljava/lang/String;\)V