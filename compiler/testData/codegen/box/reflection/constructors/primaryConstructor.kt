// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J1.java
package test;

public class J1 {}

// FILE: test/J2.java
package test;

public class J2 {
    public J2(String s) {}
    protected J2(int x) {}
    private J2(double x) {}
}

// FILE: box.kt
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.reflect.full.*
import test.*

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

    assertNotNull(En::class.primaryConstructor)

    assertNull(I::class.primaryConstructor)
    assertNull(O::class.primaryConstructor)
    assertNull(C.Companion::class.primaryConstructor)

    assertNull(object {}::class.primaryConstructor)

    assertNull(J1::class.primaryConstructor)
    assertNull(J2::class.primaryConstructor)

    return "OK"
}
