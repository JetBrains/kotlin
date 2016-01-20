class Wrapper<T>(var x: T)

inline fun <reified T> change(w: Wrapper<T>, x: Any?) {
    if (x is T) {
        w.x = <!DEBUG_INFO_SMARTCAST!>x<!>
    }
}
