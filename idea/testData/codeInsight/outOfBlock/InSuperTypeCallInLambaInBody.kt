// FALSE

open class A(a: () -> Unit)

class B: A({ "1"<caret> })