// SKIP_APT
// ISSUE: KT-70764 (apt failure)
package test

interface KotlinInterface

object Impl : KotlinInterface

fun useMethod() = MethodWithTypeParameter.method(Impl)
