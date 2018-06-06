// !LANGUAGE: +InlineClasses

class Inv<T>

inline class AsList<T>(val list: List<T>)
inline class UInt(val value: Int)

object Test {
    fun withInlineClassArgument(a: AsList<UInt>) {}
    fun withListOfInlineClassArgument(a: AsList<List<UInt>>) {}
    fun withInnerGenericInlineClass(a: AsList<AsList<List<UInt>>>) {}
}

// method: Test::withInlineClassArgument
// jvm signature: (Ljava/util/List;)V
// generic signature: (Ljava/util/List<LUInt;>;)V

// method: Test::withListOfInlineClassArgument
// jvm signature: (Ljava/util/List;)V
// generic signature: (Ljava/util/List<+Ljava/util/List<LUInt;>;>;)V

// method: Test::withInnerGenericInlineClass
// jvm signature: (Ljava/util/List;)V
// generic signature: (Ljava/util/List<LAsList<Ljava/util/List<Ljava/lang/Integer;>;>;>;)V
