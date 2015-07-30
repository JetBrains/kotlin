<error>@file:kotlin.deprecated("message")</error>
@file:suppress(<error>BAR</error>)
<error>@file:suppress(BAZ)</error>

<error><error>@<error>k</error>otlin.deprecated("message")</error></error>
<error>@<error>s</error>uppress(<error>BAR</error>)</error>
<error>@<error>s</error>uppress(BAZ)</error>

@file:myAnnotation(1, "string")
@file:boo.myAnnotation(1, <error>BAR</error>)
@file:myAnnotation(N, BAZ)

@<error>m</error>yAnnotation(1, "string")
@<error>b</error>oo.myAnnotation(1, "string")
@<error>m</error>yAnnotation(N, BAZ)

package boo

val BAZ = "baz"
val N = 0

target(AnnotationTarget.FILE)
annotation(repeatable = true, retention = AnnotationRetention.SOURCE) class myAnnotation(val i: Int, val s: String)
