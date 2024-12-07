// RUN_PIPELINE_TILL: BACKEND
open class Inv<T>(val value: String)

fun <T : Inv<*>?, F: Inv<out Any>?, G : Inv<*>> test1(t: T, f: F, g: G?) {
    if (t != null && f != null && g != null) {
        t.value
        f.value
        g.value
    }
}

// Actually, this behavior is very questional as capturing shouldn't be performed deeper than for 1 level
// But we preserve behavior of old inference here for now
fun <T : K, K : Inv<*>?> test2(t: T) {
    if (t != null) {
        t.value
    }
}

fun <T : Inv<K>?, K : Inv<*>?> test3(t: T) {
    if (t != null) {
        t.value
    }
}