// !DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.reflect.KClass

fun f1(x: String?): String {
    x!!::hashCode
    return <!DEBUG_INFO_SMARTCAST!>x<!>
}

fun f2(y: String?): String {
    val f: KClass<*> = (y ?: return "")::class
    return <!DEBUG_INFO_SMARTCAST!>y<!>
}
