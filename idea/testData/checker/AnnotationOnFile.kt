<error>@file:kotlin.Deprecated("message")</error>
@file:Suppress(<error>BAR</error>)
<error>@file:Suppress(BAZ)</error>

<error><error>@<error>k</error>otlin.Deprecated("message")</error></error>
<error>@<error>S</error>uppress(<error>BAR</error>)</error>
<error>@<error>S</error>uppress(BAZ)</error>

@file:myAnnotation(1, "string")
@file:boo.myAnnotation(1, <error>BAR</error>)
@file:myAnnotation(N, BAZ)

@<error>m</error>yAnnotation(1, "string")
@<error>b</error>oo.myAnnotation(1, "string")
@<error>m</error>yAnnotation(N, BAZ)

package boo

const val BAZ = "baz"
const val N = 0

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class myAnnotation(val i: Int, val s: String)
