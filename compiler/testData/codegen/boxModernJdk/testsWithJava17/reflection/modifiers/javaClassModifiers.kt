// WITH_REFLECT

// FILE: Sealed.java
public abstract sealed class Sealed permits NonSealed {}

// FILE: NonSealed.java
public non-sealed class NonSealed extends Sealed {}

// FILE: box.kt
import kotlin.test.assertTrue
import kotlin.test.assertFalse

fun box(): String {
    assertTrue(Sealed::class.isSealed)
    assertFalse(Sealed::class.isFinal)
    assertFalse(Sealed::class.isOpen)
    assertFalse(Sealed::class.isAbstract)

    assertFalse(NonSealed::class.isSealed)
    assertFalse(NonSealed::class.isFinal)
    assertTrue(NonSealed::class.isOpen)
    assertFalse(NonSealed::class.isAbstract)

    return "OK"
}
