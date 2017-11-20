actual class Foo actual constructor(param: String) {
    actual var property: Int = param.length

    actual fun <T> function(p: List<T>): T = p.first()
}
