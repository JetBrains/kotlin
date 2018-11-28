sealed class Sealed {
    open class OpenSubSealedNestedNoChild() : Sealed()

    open class SubSealedNestedWithChild  : Sealed()

    class SubSealedNestedChild() : SubSealedNestedWithChild()

    final class FinalSubSealedNested() : Sealed()

    class SubSealedNested() : Sealed()
}

class SubSealedParentheses() : Sealed()

class SubSealedNoParentheses : Sealed()

open class SubSealedWithChild : Sealed()

class SubSealedChild : SubSealedWithChild()

class SubSealedParameters(val value : String) : Sealed()

object SubSealedObject() : Sealed()

class SubSealedObjectWithPrimaryConstructor(parameter: String) : Sealed()

class SubSealedObjectWithSecondaryConstructor() : Sealed() {
    constructor(parameter: String): this()
}

object AlreadyObject: Sealed()

class SubSealedWithCompanion: Sealed() {
    companion object
}

class SubSealedWithInner: Sealed() {
    inner class Inner
}

class SubSealedWithNested: Sealed() {
    class Nested
}

class SubSealedWithFunction: Sealed() {
    fun internalFunction() { }
}