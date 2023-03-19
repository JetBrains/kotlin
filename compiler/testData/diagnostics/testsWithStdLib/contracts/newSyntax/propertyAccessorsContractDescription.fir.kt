// LANGUAGE: +ContractSyntaxV2
import kotlin.contracts.*

class A {
    var x: Int = 0
        get() = f(x)
        set(value) contract <!CONTRACT_NOT_ALLOWED!>[returns() implies (value != null)]<!> {
        field = value + 1
    }

    var y: Double = 0.0
        get() = g(y)
        set(value) contract <!CONTRACT_NOT_ALLOWED!>[returns() implies (value != null)]<!> {
        field = value * 2
    }

    fun f(arg: Int) = arg * arg
    fun g(arg: Double) = arg / 2
}
