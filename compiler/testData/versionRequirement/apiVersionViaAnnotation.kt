@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package test

import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind

@RequireKotlin("1.1", "message", DeprecationLevel.WARNING, RequireKotlinVersionKind.API_VERSION, 42)
class Klass

class Konstructor @RequireKotlin("1.1", "message", DeprecationLevel.WARNING, RequireKotlinVersionKind.API_VERSION, 42) constructor()

@RequireKotlin("1.1", "message", DeprecationLevel.WARNING, RequireKotlinVersionKind.API_VERSION, 42)
typealias Typealias = String

@RequireKotlin("1.1", "message", DeprecationLevel.WARNING, RequireKotlinVersionKind.API_VERSION, 42)
fun function() {}

@RequireKotlin("1.1", "message", DeprecationLevel.WARNING, RequireKotlinVersionKind.API_VERSION, 42)
val property = ""
