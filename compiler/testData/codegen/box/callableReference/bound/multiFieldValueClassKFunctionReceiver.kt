// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// ISSUE: KT-86207

import kotlin.reflect.KFunction0

value class Point(val x: Int, val y: Int)

fun Point.render(): String = "P:$x:$y"

fun box(): String {
    val ref: KFunction0<String> = Point(1, 2)::render

    val result = ref()
    if (result != "P:1:2") return "FAIL result"
    if (ref.name != "render") return "FAIL name"

    return "OK"
}
