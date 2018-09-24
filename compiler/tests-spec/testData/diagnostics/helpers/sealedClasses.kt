sealed class _SealedClass
data class _SealedChild1(val number: Int) : _SealedClass()
data class _SealedChild2(val e1: Int, val e2: Int) : _SealedClass()
data class _SealedChild3(val m1: Int, val m2: Int) : _SealedClass()

sealed class _SealedClassWithObjects
object _SealedWithObjectsChild1 : _SealedClassWithObjects()
object _SealedWithObjectsChild2 : _SealedClassWithObjects()
object _SealedWithObjectsChild3 : _SealedClassWithObjects()

sealed class _SealedClassSingle
data class _SealedSingleChild1(val number: Int) : _SealedClassSingle()

sealed class _SealedClassSingleWithObject
object _SealedSingleWithObjectChild1: Expr3() {}

sealed class _SealedClassEmpty

sealed class _SealedClassWithMethods
class _SealedWithMethodsChild1() : _SealedClassWithMethods() {
    fun m1() = this.hashCode().toString()
}
class _SealedWithMethodsChild2() : _SealedClassWithMethods() {
    fun m2() = this.hashCode().toString()
}
class _SealedWithMethodsChild3() : _SealedClassWithMethods() {
    fun m3() = this.hashCode().toString()
}

sealed class _SealedClassMixed
data class _SealedMixedChild1(val number: Int) : _SealedClassMixed()
data class _SealedMixedChild2(val e1: Int, val e2: Int) : _SealedClassMixed()
data class _SealedMixedChild3(val m1: Int, val m2: Int) : _SealedClassMixed()
object _SealedMixedChildObject1 : _SealedClassMixed()
object _SealedMixedChildObject2 : _SealedClassMixed()
object _SealedMixedChildObject3 : _SealedClassMixed()