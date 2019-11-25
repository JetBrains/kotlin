import test.*
import test.SubClass.InnerStatic as ClassInnerStatic // No error - does not force supertype resolution for SubClass
import test.SubObject.InnerStatic // TODO report error - imports from object force resolution of its supertypes

fun test() {
    SubClass().Inner() // Error - dispatch receiver misses supertype
    SubClass.InnerStatic() // No error - does not force supertype resolution for SubClass
    SubObject.InnerStatic() // TODO report error - for objects supertypes are resolved here
}
