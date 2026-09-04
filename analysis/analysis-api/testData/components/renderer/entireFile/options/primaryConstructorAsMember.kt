// NO_PRIMARY_CONSTRUCTOR_IN_CLASS_HEADER
class Simple(val x: Int, y: String)

class WithSecondary(val x: Int) {
    constructor() : this(0)
}

class WithAnnotatedConstructor @Ctor constructor(val x: Int)

@Target(AnnotationTarget.CONSTRUCTOR)
annotation class Ctor
