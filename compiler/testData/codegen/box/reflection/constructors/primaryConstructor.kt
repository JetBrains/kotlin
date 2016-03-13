// WITH_REFLECT

import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.reflect.*

class OnlyPrimary

class PrimaryWithSecondary(val s: String) {
    constructor(x: Int) : this(x.toString())

    override fun toString() = s
}

class OnlySecondary {
    constructor(s: String)
}

class TwoSecondaries {
    constructor(s: String)
    constructor(d: Double)
}

enum class En

interface I
object O
class C {
    companion object
}

fun box(): String {
    val p1 = OnlyPrimary::class.primaryConstructor
    assertNotNull(p1)
    assert(p1!!.call() is OnlyPrimary)

    val p2 = PrimaryWithSecondary::class.primaryConstructor
    assertNotNull(p2)
    assert(p2!!.call("beer").toString() == "beer")

    val p3 = OnlySecondary::class.primaryConstructor
    assertNull(p3)

    val p4 = TwoSecondaries::class.primaryConstructor
    assertNull(p4)

    assertNotNull(En::class.primaryConstructor) // TODO: maybe primaryConstructor should be null for enum classes

    assertNull(I::class.primaryConstructor)
    assertNull(O::class.primaryConstructor)
    assertNull(C.Companion::class.primaryConstructor)

    return "OK"
}
