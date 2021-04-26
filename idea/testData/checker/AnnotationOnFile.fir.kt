@file:kotlin.Deprecated("message")
@file:Suppress(<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: BAR">BAR</error>)
@file:Suppress(BAZ)

@<error descr="Expecting \"file:\" prefix for file annotations">k</error>otlin.Deprecated("message")
@<error descr="Expecting \"file:\" prefix for file annotations">S</error>uppress(<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: BAR">BAR</error>)
@<error descr="Expecting \"file:\" prefix for file annotations">S</error>uppress(BAZ)

@file:myAnnotation(1, "string")
@file:boo.myAnnotation(1, <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: BAR">BAR</error>)
@file:myAnnotation(N, BAZ)

@<error descr="Expecting \"file:\" prefix for file annotations">m</error>yAnnotation(1, "string")
@<error descr="Expecting \"file:\" prefix for file annotations">b</error>oo.myAnnotation(1, "string")
@<error descr="Expecting \"file:\" prefix for file annotations">m</error>yAnnotation(N, BAZ)

package boo

const val BAZ = "baz"
const val N = 0

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class myAnnotation(val i: Int, val s: String)
