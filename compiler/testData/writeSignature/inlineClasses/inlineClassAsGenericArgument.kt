// LANGUAGE: +InlineClasses

class Inv<T>

inline class UInt(val value: Int)

object Test {
    fun asNotNullTypeArgument(i: Inv<UInt>) {}
    fun asInnerTypeArgument(i: Inv<Inv<UInt>>) {}
}

// method: Test::asNotNullTypeArgument
// jvm signature: (LInv;)V
// generic signature: (LInv<LUInt;>;)V

// method: Test::asInnerTypeArgument
// jvm signature: (LInv;)V
// generic signature: (LInv<LInv<LUInt;>;>;)V