// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FULL_JDK

interface IActing {
    fun act(): String
}

class CActing(val value: String = "OK") : IActing {
    override fun act(): String = value
}

// final so no need in delegate field
class Test(val acting: CActing = CActing()) : IActing by acting {
}

// even if open so we don't need delegate field
open class Test2(open val acting: CActing = CActing()) : IActing by acting {
}

// even if open the backing field is final, so we don't need delegate field
class Test3() : Test2() {
    override val acting = CActing("OKOK")
}

fun box(): String {
    try {
        Test::class.java.getDeclaredField("\$\$delegate_0")
        return "\$\$delegate_0 field generated for class Test but should not"
    }
    catch (e: NoSuchFieldException) {
        // ok
    }

    try {
        Test2::class.java.getDeclaredField("\$\$delegate_0")
        return "\$\$delegate_0 field generated for class Test but should not"
    }
    catch (e: NoSuchFieldException) {
        // ok
    }

    if (Test3().acting.act() != "OKOK") return "Fail Test3"

    val test = Test()
    return test.act()
}
