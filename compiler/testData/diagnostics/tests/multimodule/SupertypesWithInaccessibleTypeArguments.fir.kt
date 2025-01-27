// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidUsingSupertypesWithInaccessibleContentInTypeArguments

// MODULE: start
// FILE: start.kt

interface InaccessibleConcreteSuperType
interface InaccessibleGenericSuperType<T>

// MODULE: middle(start)
// FILE: middle.kt

interface Box<T>

interface BoxedConcreteType : Box<InaccessibleConcreteSuperType>
interface BoxedGenericType : Box<InaccessibleGenericSuperType<Nothing>>
interface BoxedGenericTypeWithGeneric<T> : Box<T>
interface ConcreteSuperType : InaccessibleConcreteSuperType

// MODULE: end(middle)
// FILE: end.kt

interface BoxedConcreteTypeImplementation : BoxedConcreteType

interface BoxedGenericTypeImplementation : BoxedGenericType

<!MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT!>interface BoxedGenericTypeWithGenericImplementation<!> : BoxedGenericTypeWithGeneric<ConcreteSuperType>
