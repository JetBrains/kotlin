// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

// MODULE: lib1v1
// FILE: lib1.kt
open class ClassWithAddedFinal {
   fun test(): Int = 1
}
open class ClassWithAddedOpen {
   fun test(): Int = 1
}
abstract class ClassWithAddedAbstract {
   fun test(): Int = 1
}

// MODULE: lib1v2
// FILE: lib1.kt
open class ClassWithAddedFinal {
    fun foo(): Int = 2
    fun test() = foo()
}
open class ClassWithAddedOpen {
    open fun foo(): Int = 2
    fun test() = foo()
}
abstract class ClassWithAddedAbstract {
    abstract fun foo(): Int
    fun test() = foo()
}

// MODULE: lib2(lib1v1)
class ClassWithPublicShadowingFinal : ClassWithAddedFinal() {
   public fun foo() = 3
}
class ClassWithPublicShadowingOpen : ClassWithAddedOpen() {
   public fun foo() = 3
}
class ClassWithPublicShadowingAbstract : ClassWithAddedAbstract() {
   public fun foo() = 3
}

class ClassWithProtectedShadowingFinal : ClassWithAddedFinal() {
   protected fun foo() = 3
}
class ClassWithProtectedShadowingOpen : ClassWithAddedOpen() {
   protected fun foo() = 3
}
class ClassWithProtectedShadowingAbstract : ClassWithAddedAbstract() {
   protected fun foo() = 3
}

class ClassWithInternalPAShadowingFinal : ClassWithAddedFinal() {
   @PublishedApi internal fun foo() = 3
}
class ClassWithInternalPAShadowingOpen : ClassWithAddedOpen() {
   @PublishedApi internal fun foo() = 3
}
class ClassWithInternalPAShadowingAbstract : ClassWithAddedAbstract() {
   @PublishedApi internal fun foo() = 3
}

class ClassWithInternalShadowingFinal : ClassWithAddedFinal() {
   internal fun foo() = 3
}
class ClassWithInternalShadowingOpen : ClassWithAddedOpen() {
   internal fun foo() = 3
}
class ClassWithInternalShadowingAbstract : ClassWithAddedAbstract() {
   internal fun foo() = 3
}

class ClassWithPrivateShadowingFinal : ClassWithAddedFinal() {
   private fun foo() = 3
}
class ClassWithPrivateShadowingOpen : ClassWithAddedOpen() {
   private fun foo() = 3
}
class ClassWithPrivateShadowingAbstract : ClassWithAddedAbstract() {
   private fun foo() = 3
}

// MODULE: main(lib1v2, lib2)
import kotlin.test.*

fun box(): String {
    expectClassVerificationError { ClassWithPublicShadowingFinal().test() }
    expectSuccess(3) { ClassWithPublicShadowingOpen().test() }
    expectSuccess(3) { ClassWithPublicShadowingAbstract().test() }

    expectClassVerificationError { ClassWithProtectedShadowingFinal().test() }
    expectSuccess(3) { ClassWithProtectedShadowingOpen().test() }
    expectSuccess(3) { ClassWithProtectedShadowingAbstract().test() }

    expectClassVerificationError { ClassWithInternalPAShadowingFinal().test() }
    expectSuccess(3) { ClassWithInternalPAShadowingOpen().test() }
    expectSuccess(3) { ClassWithInternalPAShadowingAbstract().test() }

    expectSuccess(2) { ClassWithInternalShadowingFinal().test() }
    expectSuccess(2) { ClassWithInternalShadowingOpen().test() }
    expectLinkageError {
        ClassWithInternalShadowingAbstract().test()
    }

    expectSuccess(2) { ClassWithPrivateShadowingFinal().test() }
    expectSuccess(2) { ClassWithPrivateShadowingOpen().test() }
    expectLinkageError {
        ClassWithPrivateShadowingAbstract().test()
    }

    return "OK"
}

private fun expectSuccess(expected: Any?, block: () -> Any?) {
    val actual = block()
    assertEquals(expected, actual)
}

private fun expectLinkageError(block: () -> Any?) {
    try {
        val _ = block()
    } catch (ex: LinkageError) {
        return
    }
    fail("Expected to fail, succeeded instead")
}

private fun expectClassVerificationError(block: () -> Any?) {
    try {
        val _ = block()
    } catch (ex: IncompatibleClassChangeError) {
        return
    } catch (ex: VerifyError) {
        return
    }
    fail("Expected to fail, succeeded instead")
}
