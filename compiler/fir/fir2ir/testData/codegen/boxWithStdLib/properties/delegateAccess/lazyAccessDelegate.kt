import kotlin.reflect.KProperty

class LazyAccess<T, K>(var field: K, setup: K.() -> T) {
    private val outer by lazy {
        field.setup()
    }

    operator fun getValue(self: Any?, prop: KProperty<*>): T {
        return outer
    }
}

fun <K, T> K.withLazyAccess(setup: K.() -> T) = LazyAccess(this, setup)

var marker = false

val number by 10.withLazyAccess {
    marker = true
    "[$this]"
}

fun box(): String {
    number#field += 20

    if (marker) {
        return "FAIL: marker is $marker"
    }

    if (number != "[30]" || !marker) {
        return "FAIL: number is $number, marker is $marker"
    }

    marker = false

    number#field = 100

    if (number != "[30]" || marker) {
        return "FAIL: number is $number, marker is $marker"
    }

    return "OK"
}
