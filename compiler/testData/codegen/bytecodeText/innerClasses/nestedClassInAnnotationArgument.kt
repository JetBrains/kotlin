// FILE: annotations.kt

import kotlin.reflect.*

annotation class Anno(
    val k: KClass<*>,
    val e: C.NestedEnum,
    val a: C.NestedAnno,
)

annotation class AnnoWithDefault(val k: KClass<*> = Nested0::class) {
    class Nested0
}

class C {
    class Nested1

    enum class NestedEnum { E }

    annotation class NestedAnno(val k: KClass<*>) {
        class Nested2
    }
}

// FILE: usage.kt

interface I {
    @Anno(
        C.Nested1::class,
        C.NestedEnum.E,
        C.NestedAnno(C.NestedAnno.Nested2::class),
    )
    @AnnoWithDefault
    fun foo(): String = "OK"
}

// @I.class:
// 5 INNERCLASS
// 1 INNERCLASS C\$Nested1 C Nested1
// 1 INNERCLASS C\$NestedEnum C NestedEnum
// 1 INNERCLASS C\$NestedAnno C NestedAnno
// 1 INNERCLASS C\$NestedAnno\$Nested2 C\$NestedAnno Nested2
// 1 INNERCLASS I\$DefaultImpls I DefaultImpls
