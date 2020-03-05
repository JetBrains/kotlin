// !DUMP_CFG

open class Base
class Sub(val data: Base): Base()

fun Sub.isOk() = true

fun check(base: Base): Base =
    when {
        (base as? Sub)?.isOk() == true -> {
            base.data
        }
        else -> {
            base
        }
    }


