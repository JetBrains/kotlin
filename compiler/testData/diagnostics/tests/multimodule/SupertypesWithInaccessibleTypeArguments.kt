// FIR_IDENTICAL

// MODULE: start
// FILE: start.kt

interface InaccessibleConcreteSuperType
interface InaccessibleGenericSuperType<T>

// MODULE: middle(start)
// FILE: middle.kt

interface Box<T>

interface BoxedConcreteType : Box<InaccessibleConcreteSuperType>
interface BoxedGenericType : Box<InaccessibleGenericSuperType<Nothing>>

// MODULE: end(middle)
// FILE: end.kt

// a MISSING_DEPENDENCY_(SUPER)CLASS-like error should be reported here
interface BoxedConcreteTypeImplementation : BoxedConcreteType

// a MISSING_DEPENDENCY_(SUPER)CLASS-like error should be reported here
interface BoxedGenericTypeImplementation : BoxedGenericType
