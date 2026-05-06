object SomeObject {
    fun foo(): String {
        return "OK"
    }
}

typealias OnSomeObject<T> = SomeObject

fun box(): String {
    val withTypeArgument = OnSomeObject<Any>::foo
    val withoutTypeArgument = OnSomeObject::foo
    return if (withTypeArgument(SomeObject) == withoutTypeArgument()) withoutTypeArgument() else "fail"
}
