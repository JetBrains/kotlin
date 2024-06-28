class OuterClass<T1> : SuperForOuter() {
    class NestedClass<T2> : Another()
    typealias Nes<caret>tedType<T> = NestedClass<T>
}

open class Another
open class SuperForOuter