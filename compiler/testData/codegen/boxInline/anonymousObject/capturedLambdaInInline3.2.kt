package test

inline fun bar(crossinline y: () -> String) = {
    { { call(y) }() }()
}

public inline fun <T> call(crossinline f: () -> T): T = {{ f() }()}()