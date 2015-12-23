interface A
class B : A {
    operator fun invoke() = this
}

class C : A

operator fun C.invoke(): B = B()

fun foo(arg: A): B? {
    if (arg is B) return <!DEBUG_INFO_SMARTCAST!>arg<!>()

    if (arg is C) return <!DEBUG_INFO_SMARTCAST!>arg<!>()

    return null
}

