// LANGUAGE: +CollectionLiterals
// WITH_PLATFORM_LIBS
import platform.darwin.*
import platform.Foundation.*

// Keep these calls passing until KT-81722 changes collection literal desugaring.
// Fixing the regression will be in scope of KT-82852.
fun objcNamedSpreadStillWorks() =
    NSAssertionHandler().handleFailureInFunction("zzz", "zzz", 0, null, args = *[1, 2, 3])

fun cVariadicSpreadStillWorks() = NSLog("zzz", *[1, 2, 3])
