// FILE: propertyReferences.kt
object Delegate {
    operator fun getValue(thisRef: Any?, kProp: Any) = 1
    operator fun setValue(thisRef: Any?, kProp: Any, value: Int) {}
}

open class C {
    var varWithPrivateSet: Int = 1
        private set
    var varWithProtectedSet: Int = 1
        protected set
}

val valWithBackingField = 1

val test_valWithBackingField = ::valWithBackingField

var varWithBackingField = 1

val test_varWithBackingField = ::varWithBackingField

var varWithBackingFieldAndAccessors = 1
    get() = field
    set(value) { field = value }

val test_varWithBackingFieldAndAccessors = ::varWithBackingFieldAndAccessors

val valWithAccessors
    get() = 1

val test_valWithAccessors = ::valWithAccessors

var varWithAccessors
    get() = 1
    set(value) {}

val test_varWithAccessors = ::varWithAccessors

val delegatedVal by Delegate

val test_delegatedVal = ::delegatedVal

var delegatedVar by Delegate

val test_delegatedVar = ::delegatedVar

val constVal = 1

val test_constVal = ::constVal

val test_J_CONST = J::CONST
val test_J_nonConst = J::nonConst

val test_varWithPrivateSet = C::varWithPrivateSet
val test_varWithProtectedSet = C::varWithProtectedSet

// FILE: J.java
public class J {
    public static final int CONST = 1;
    public static int nonConst = 2;
}