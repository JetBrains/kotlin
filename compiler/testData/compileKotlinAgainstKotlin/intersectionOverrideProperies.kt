// TARGET_BACKEND: JVM
// FILE: A.kt
package a

interface IrSymbol {
    val owner: Any
}

interface IrFunction
interface IrSimpleFunction : IrFunction {
    val name: String
}

interface IrFunctionSymbol : IrSymbol {
    override val owner: IrFunction
}

interface IrBindableSymbol<B : Any> : IrSymbol {
    override val owner: B
}

interface IrSimpleFunctionSymbol : IrFunctionSymbol, IrBindableSymbol<IrSimpleFunction>

// FILE: B.kt
import a.*

fun foo(x: IrSimpleFunctionSymbol): String {
    return x.owner.name
}

fun box(): String {
    return foo(object : IrSimpleFunctionSymbol {
        override val owner: IrSimpleFunction
            get() = object : IrSimpleFunction {
                override val name: String
                    get() = "OK"
            }
    })
}
