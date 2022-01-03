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

// method: Test::withInlineClassArgumentOut-bGQ91Ds
// jvm signature: (Ljava/util/List;)V
// generic signature: (Ljava/util/List<+TT;>;)V

// method: Test::withInlineClassArgumentIn-1sHIm6c
// jvm signature: (Ljava/lang/Comparable;)V
// generic signature: (Ljava/lang/Comparable<-TT;>;)V

// method: Test::withListOfInlineClassArgument-bGQ91Ds
// jvm signature: (Ljava/util/List;)V
// generic signature: (Ljava/util/List<+TT;>;)V

// method: Test::withComparableOfInlineClassArgument-1sHIm6c
// jvm signature: (Ljava/lang/Comparable;)V
// generic signature: (Ljava/lang/Comparable<-TT;>;)V

// method: Test::withInnerGenericInlineClassOut-bGQ91Ds
// jvm signature: (Ljava/util/List;)V
// generic signature: (Ljava/util/List<+TT;>;)V

// method: Test::withInnerGenericInlineClassIn-1sHIm6c
// jvm signature: (Ljava/lang/Comparable;)V
// generic signature: (Ljava/lang/Comparable<-TT;>;)V