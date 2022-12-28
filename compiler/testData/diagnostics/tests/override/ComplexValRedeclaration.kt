// !DIAGNOSTICS: -TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER
package override.generics

abstract class MyAbstractClass<T> {
    abstract val pr : T
}

abstract class MyLegalAbstractClass2<T>(t : T) : MyAbstractClass<Int>() {
    <!CONFLICTING_OVERLOADS!>val <R> pr : T<!> = t
}