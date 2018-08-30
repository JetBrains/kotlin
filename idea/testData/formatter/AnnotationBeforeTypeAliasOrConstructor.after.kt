@Target(AnnotationTarget.TYPEALIAS, AnnotationTarget.CONSTRUCTOR)
annotation class Ann

@Ann
typealias Alias = Int

class C @Ann constructor(val x: Int) {
    @Ann
    constructor() : this(42) {
    }
}