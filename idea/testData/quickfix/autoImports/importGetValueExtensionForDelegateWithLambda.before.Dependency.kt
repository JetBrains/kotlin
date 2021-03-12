package base

class MyDelegate<T>(init: () -> T) {
    var value: T = init()
}

operator fun <T> MyDelegate<T>.getValue(thisObj: Any?, property: kotlin.reflect.KProperty<*>): T = value