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
interface VeryConcreteSuperType : InaccessibleConcreteSuperType

// MODULE: end(middle)
// FILE: end.kt

// Type argument of 2nd level supertype is inaccessible
interface BoxedConcreteTypeImplementation : BoxedConcreteType

// Type argument of 2nd level supertype is inaccessible
interface BoxedGenericTypeImplementation : BoxedGenericType

// Supertype of type argument of direct supertype is inaccessible
<!MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT!>interface BoxedGenericTypeWithGenericImplementation<!> : BoxedGenericTypeWithGeneric<ConcreteSuperType>

// Type argument of supertype of type argument of direct supertype is inaccessible
interface BoxedGenericTypeWithBoxedImplementation : BoxedGenericTypeWithGeneric<BoxedConcreteType>

// 2nd level supertype of type argument of direct supertype is inaccessible
<!MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT!>interface VeryBoxedGenericTypeWithGenericImplementation<!> : BoxedGenericTypeWithGeneric<VeryConcreteSuperType>
