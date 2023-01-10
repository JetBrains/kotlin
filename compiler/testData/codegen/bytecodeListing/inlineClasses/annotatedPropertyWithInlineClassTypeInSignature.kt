// WITH_STDLIB
inline class Z(val x: Int) {
    @Anno
    val member: Int get() = x
}

annotation class Anno

@Anno
val Z.topLevel: Int get() = 0

@Anno
val returnType: Z get() = Z(0)

class C {
    @Anno
    val Z.memberExtension: Int get() = 0

    @Anno
    val returnType: Z get() = Z(0)

    @Anno
    internal val Z.internal: Int get() = 0
}
