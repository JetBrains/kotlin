sealed class Sealed() {
    open class SubSealedNestedNoChild(): Sealed()

    open class SubSealedNestedWithChild(): Sealed()

    class SubSealedNestedChild(): SubSealedNestedWithChild()
}

class SubSealedParentheses(): Sealed()

class SubSealedNoParentheses: Sealed()

class SubSealedWithChild: Sealed()

class SubSealedChild: SubSealedWithChild()

class SubSealedParameters(val value: String): Sealed()

object SubSealedObject: Sealed()