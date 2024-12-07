// NO_CHECK_SOURCE_VS_BINARY
// Type-alias info is lost during serialization in K1
package test

class Context

typealias SuspendWithContext = suspend Context.() -> Unit

fun foo(f: SuspendWithContext) {}