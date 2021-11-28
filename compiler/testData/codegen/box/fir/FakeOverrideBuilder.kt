// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: FakeOverrideBuilder_lib.kt

class IrPropertySymbolImpl :
    IrBindableSymbolBase<IrProperty>(),
    IrPropertySymbol

abstract class IrBindableSymbolBase<B : IrSymbolOwner> :
    IrBindableSymbol<B>, IrSymbolBase() {

    private var _owner: B? = null
    override val owner: B
        get() = _owner ?: throw IllegalStateException("")

    override fun bind(owner: B) {
        if (_owner == null) {
            _owner = owner
        } else {
            throw IllegalStateException("")
        }
    }
}

abstract class IrSymbolBase : IrSymbol

interface IrPropertySymbol : IrBindableSymbol<IrProperty>

interface IrBindableSymbol<B : IrSymbolOwner> : IrSymbol {
    override val owner: B

    fun bind(owner: B)
}

interface IrSymbol {
    val owner: IrSymbolOwner
}

interface IrSymbolOwner {
    val symbol: IrSymbol
}

class IrProperty(override val symbol: IrPropertySymbol, val name: String) : IrSymbolOwner

// MODULE: main(lib)
// FILE: FakeOverrideBuilder_main.kt

fun link(declaration: IrSymbolOwner) {
    val tempSymbol = IrPropertySymbolImpl()
    tempSymbol.bind(declaration as IrProperty)
}

fun box(): String {
    val symbol = IrPropertySymbolImpl()
    val property = IrProperty(symbol, "OK")
    symbol.bind(property)
    link(property)
    return symbol.owner.name
}
