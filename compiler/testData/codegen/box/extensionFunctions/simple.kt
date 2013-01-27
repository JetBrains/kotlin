fun StringBuilder.first() = this.charAt(0)

fun foo() = StringBuilder("foo").first()

fun box() = if (foo() == 'f') "OK" else "Fail ${foo()}"
