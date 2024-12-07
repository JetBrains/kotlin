import kotlin.reflect.KProperty1

fun <T> genericFun(value: T): T {
    class Local(val item: T)

    val unwrapItem: KProperty1<Local, T> = Local::item

    return unwrapItem(Local(value))
}

fun box(): String {
    return genericFun("OK")
}