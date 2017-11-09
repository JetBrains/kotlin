@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package test

import kotlin.internal.RequireKotlin

@RequireKotlin("1.1", "message", DeprecationLevel.WARNING, errorCode = 42)
class Klass

class Konstructor @RequireKotlin("1.1", "message", DeprecationLevel.WARNING, errorCode = 42) constructor()

@RequireKotlin("1.1", "message", DeprecationLevel.WARNING, errorCode = 42)
typealias Typealias = String

@RequireKotlin("1.1", "message", DeprecationLevel.WARNING, errorCode = 42)
fun function() {}

@RequireKotlin("1.1", "message", DeprecationLevel.WARNING, errorCode = 42)
val property = ""
