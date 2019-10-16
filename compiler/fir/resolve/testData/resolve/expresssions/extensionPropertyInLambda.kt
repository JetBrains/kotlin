class C<T>(var x: T)

var <T> C<T>.y
    get() = x
    set(v) {
        x = v
    }

fun use(f: () -> String) {}

fun test1() {
    use { C("abc").y }
    use(C("abc")::y)
}