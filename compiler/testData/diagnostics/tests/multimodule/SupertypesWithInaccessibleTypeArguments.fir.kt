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

// MODULE: end(middle)
// FILE: end.kt

<!MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT!>interface BoxedConcreteTypeImplementation<!> : BoxedConcreteType

<!MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT!>interface BoxedGenericTypeImplementation<!> : BoxedGenericType
