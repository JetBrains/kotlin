// !LANGUAGE: +InlineClasses

inline class UInt(val value: Int)

inline class AsList<T>(val list: List<T>)
inline class AsCmp<T>(val cmp: Comparable<T>)

object Test {
    fun withInlineClassArgumentOut(a: AsList<UInt>) {}
    fun withInlineClassArgumentIn(a: AsCmp<UInt>) {}

    fun withListOfInlineClassArgument(a: AsList<List<UInt>>) {}
    fun withComparableOfInlineClassArgument(a: AsCmp<Comparable<UInt>>) {}

    fun withInnerGenericInlineClassOut(a: AsList<AsList<List<UInt>>>) {}
    fun withInnerGenericInlineClassIn(a: AsCmp<AsCmp<Comparable<UInt>>>) {}
}

// method: Test::withInlineClassArgumentOut-5xv5g663
// jvm signature: (Ljava/util/List;)V
// generic signature: (Ljava/util/List<LUInt;>;)V

// method: Test::withInlineClassArgumentIn-brqdr5wn
// jvm signature: (Ljava/lang/Comparable;)V
// generic signature: (Ljava/lang/Comparable<-LUInt;>;)V

// method: Test::withListOfInlineClassArgument-5xv5g663
// jvm signature: (Ljava/util/List;)V
// generic signature: (Ljava/util/List<+Ljava/util/List<LUInt;>;>;)V

// method: Test::withComparableOfInlineClassArgument-brqdr5wn
// jvm signature: (Ljava/lang/Comparable;)V
// generic signature: (Ljava/lang/Comparable<-Ljava/lang/Comparable<-LUInt;>;>;)V

// method: Test::withInnerGenericInlineClassOut-5xv5g663
// jvm signature: (Ljava/util/List;)V
// generic signature: (Ljava/util/List<LAsList<Ljava/util/List<LUInt;>;>;>;)V

// method: Test::withInnerGenericInlineClassIn-brqdr5wn
// jvm signature: (Ljava/lang/Comparable;)V
// generic signature: (Ljava/lang/Comparable<-LAsCmp<Ljava/lang/Comparable<LUInt;>;>;>;)V