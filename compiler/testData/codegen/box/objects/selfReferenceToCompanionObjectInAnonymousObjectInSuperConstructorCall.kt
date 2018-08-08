interface IFn {
    operator fun invoke(): String
}

abstract class Base(val fn: IFn)

class Host {
    companion object : Base(
            object : IFn {
                override fun invoke(): String = Host.ok()
            }
    ) {
        fun ok() = "OK"
    }
}

fun box() = Host.Companion.fn()