//This test is extracted from useDeclarationThatWasExperimentalWithoutMarker.kt to check warnings in output

import kotlin.math.*

fun test(p: ULong) {
    val z: ULong = p
    z.inv()
    cbrt(z.toDouble())
}
