interface IFn {
    operator fun invoke(): String
}

abstract class Base(val fn: IFn)

interface Host {
    companion object : Base(
            object : IFn {
                override fun invoke(): String = Host.ok()
            }
    ) {
        fun ok() = "OK"
    }
}

fun box() = Host.Companion.fn()