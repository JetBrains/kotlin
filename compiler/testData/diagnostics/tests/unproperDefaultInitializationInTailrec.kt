// LANGUAGE: -ProperComputationOrderOfTailrecDefaultParameters

import kotlin.reflect.KClass

fun withEffects(): String = "OK"

const val Z = "123"

enum class EnumA {
    A
}

tailrec fun foo(i: Int = 1, c: Char = '2', s: String = "1234", b: Boolean = true, d: Double = 1.0,  l: Long =  1L,  y: String = withEffects()) {
    foo(i, c, s, b, d, l, y)
}

tailrec fun foo2(x: Int = 1, y: String = withEffects(), z: String = Z) {
    foo2(x, y, z)
}

tailrec fun foo3(y: String = withEffects()) {
    foo3(y)
}

<!TAILREC_WITH_DEFAULTS!>tailrec fun foo4(x: String = withEffects(), y: String = withEffects())<!> {
    foo4(x, y)
}

<!TAILREC_WITH_DEFAULTS!>tailrec fun foo5(x: String = withEffects(), y: String = withEffects(), z: String = withEffects())<!> {
    foo5(x, y, z)
}

<!TAILREC_WITH_DEFAULTS!>tailrec fun foo6(x: String = withEffects(), y: EnumA = EnumA.A)<!> {
    foo6(x, y)
}

<!TAILREC_WITH_DEFAULTS!>tailrec fun foo7(x: String = withEffects(), y: KClass<out EnumA> = EnumA.A::class)<!> {
    foo7(x, y)
}