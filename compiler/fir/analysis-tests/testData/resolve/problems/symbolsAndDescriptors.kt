class IrClassSymbolImpl(descriptor: String? = null) :
    IrBindableSymbolBase<String>(descriptor),
    IrClassSymbol

interface IrClassSymbol : IrClassifierSymbol, IrBindableSymbol<String>

interface IrClassifierSymbol : IrSymbol, TypeConstructorMarker {
    override val descriptor: CharSequence
}

interface IrSymbol {
    val descriptor: Any
}

interface TypeConstructorMarker

interface IrBindableSymbol<out D : Any> : IrSymbol {
    override val descriptor: D
}

abstract class IrBindableSymbolBase<out D : Any>(descriptor: D?) :
    IrBindableSymbol<D>, IrSymbolBase<D>(descriptor)

abstract class IrSymbolBase<out D : Any>(
    private val _descriptor: D?
) : IrSymbol {
    override val descriptor: D
        get() = _descriptor!!
}




