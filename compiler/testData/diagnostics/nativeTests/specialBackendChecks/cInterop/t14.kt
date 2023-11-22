import kotlinx.cinterop.*

class Z(rawPtr: NativePtr): CStructVar(rawPtr)

fun foo(x: CValue<Z>) = x

fun bar() {
    staticCFunction(::foo)
}
