// FIR_IDENTICAL
// MODULE: library
// FILE: a.kt
package a

internal interface InternalInterface

public class PublicClass {
    internal fun internalMemberFun() {}

    internal companion object {}
}

internal val internalVal = ""

internal fun internalFun(s: String): String = s

internal typealias InternalTypealias = InternalInterface

// MODULE: main()(library)
// FILE: source.kt
import a.*

private fun test(i: InternalInterface): InternalTypealias {
    PublicClass().internalMemberFun()
    PublicClass.Companion

    internalFun(internalVal)

    return i
}
