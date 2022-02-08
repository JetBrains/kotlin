// !LANGUAGE: -ProhibitJvmFieldOnOverrideFromInterfaceInPrimaryConstructor
// TARGET_BACKEND: JVM
// WITH_STDLIB

interface A { val x: String }

open class B(@JvmField override val x: String): A

open class BB(x: String) : B(x)

class X(x: String) : A, BB(x) {
    override val x: String
        get() = super.x
}

fun box(): String {
    val e = X("OK")
    return e.x
}
