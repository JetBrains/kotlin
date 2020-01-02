interface A
class B : A {
    operator fun invoke() = this
}

class C : A

operator fun C.invoke(): B = B()

fun foo(arg: A): B? {
    if (arg is B) return arg()

    if (arg is C) return arg()

    return null
}

