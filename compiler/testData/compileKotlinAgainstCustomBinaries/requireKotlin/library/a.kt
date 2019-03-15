@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package a

import kotlin.internal.RequireKotlin as RK
import kotlin.internal.RequireKotlinVersionKind as K

@RK("42.33", message = "This declaration is only supported since Kotlin 42.33")
@RK("40.34", versionKind = K.API_VERSION)
@RK("45.35")
class A

@RK("42.33", message = "This declaration is only supported since Kotlin 42.33")
@RK("40.34", versionKind = K.API_VERSION)
@RK("45.35")
@RK("1.1")
fun f() {}

@RK("42.33", message = "This declaration is only supported since Kotlin 42.33")
@RK("40.34", versionKind = K.API_VERSION)
@RK("45.35")
val p = ""

@RK("42.33", message = "This declaration is only supported since Kotlin 42.33")
@RK("40.34", versionKind = K.API_VERSION)
@RK("45.35")
@RK("1.1")
typealias TA = String
