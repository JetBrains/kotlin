// DIAGNOSTICS: -UNCHECKED_CAST -UNUSED_DESTRUCTURED_PARAMETER_ENTRY -USELESS_CAST -UNUSED_PARAMETER -UNUSED_EXPRESSION

class Inv<T>(val y: T)

fun <K> takeTwoInv(x: Inv<K>, y: Inv<K>) = x.y

fun <K> takeTwoInvOut(x: Inv<out K>, y: Inv<out K>) : K = x.y

fun test1(y: Any) {
    y as Map<String, Any?>
    y as Map<*, *>
    <!DEBUG_INFO_SMARTCAST!>y<!>.forEach { (k: String, u: Any?) -> }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.collections.Map<*, *> & kotlin.collections.Map<kotlin.String, kotlin.Any?>")!>y<!>
}

fun test2(x: Any, y: Inv<String>) {
    x as Inv<String>
    x as Inv<out CharSequence>
    val z = takeTwoInv(<!DEBUG_INFO_SMARTCAST!>x<!>, y)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>z<!>
}

fun test3(x: Any, y: Inv<String>) {
    x as Inv<out CharSequence>
    x as Inv<String>
    val z = takeTwoInvOut(<!DEBUG_INFO_SMARTCAST!>x<!>, y)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>z<!>
}
