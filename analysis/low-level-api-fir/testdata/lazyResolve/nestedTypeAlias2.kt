class OuterClass<T1> {
    class NestedClass<T2>
    typealias NestedType<T> = NestedClass<T>
}

typealias ON<caret>3<T2> = OuterClass.NestedType<T2>