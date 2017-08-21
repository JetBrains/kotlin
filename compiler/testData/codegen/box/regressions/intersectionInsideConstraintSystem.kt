fun <K> id(k: K): K = k

fun <V : Any> create(compute: V?): V  = compute!!

fun test(f: String?) = create(id(f))

fun box(): String {
    return "OK"
}