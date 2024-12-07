# Fir elements

- All fir elements are listed in [`FirTree.kt`](src/org/jetbrains/kotlin/fir/tree/generator/FirTree.kt).
- The syntax for declaring a new element: `val foo by element(elementKind: Kind, name: String?)`.
    - By default, the generated class will be named `FirFoo`. If you specify `name = "Bar"` it will be called `FirBar` instead.
    - `kind` describes target package of an element. Available kinds:
        - `Expression` (package `fir.expression`)
        - `Declaration` (package `fir.declaration`)
        - `Reference` (package `fir.references`)
        - `TypeRef` (package `fir.types`)
        - `Other` (package `fir`)

# Content of elements

- **Fields:**
    - You can create fields with `field(..., nullable: Boolean = false, withReplace: Boolean)`.
        - if `nullable` is true, then the type of the field will be nullable.
        - if `withReplace` is true, then in the element the `replace...` method will be generated for that field.
        - if `withTransform` is true, then in the element the `transform...` method will be generated for that field.
        - in place of `...` you can pass an optional name (with `String` type), and `TypeRef` or `Element` object
        - if no `name` is passed, then it will be inferred based on the type.
        - if `TypeRef` or `Element` have type arguments, then you can use `TypeRef.withArgs(vararg types: TypeRef)`.
    - Also, you can create fields with lists of some types.
        - Syntax: `fieldList([name: String], element: TypeRef)` (if no name is specified, it will be inferred based on the type of
          `element`).
    - To add the field to the node being configured, you should call the infix `+` operator:
      `+fieldList("catches", catchClause)`.
    - Also, you can use `symbol(symbolTypeName: String)` to create a field named `symbol` with a lying in
      the `org.jetbrains.kotlin.fir.symbols` package.
    - Some predefined fields are listed in [`FieldSets.kt`](src/org/jetbrains/kotlin/fir/tree/generator/FirTree.kt).
- Parent (base) nodes are added by separate calls to `parent(foo)`
    - If not a single parent element was declared, then the generated element will be a direct inheritor of `FirElement`.
- If your node has some `transform...` methods, and you want to add methods for transforming all other children, you should call
  `needTransformOtherChildren()`.
- If an element has type parameters, you should declare them using `+param(typeParameterName: String, [upperBound: TypeRef])`.
- If an element inherits another element with type parameters, you should match those parameters with concrete types using
  `parent(foo.withArgs(...))`.

# Implementations

- If an element has no inheritors, then it will have a default implementation. Otherwise, you should declare an implementation that you
  want.
- All implementations are described in `ImplementationConfigurator.kt`
- Syntax:
    - `impl(element: Element, [name: String]) {...}` describes the configuration of the element with name `name` (if there is no name,
      then it would be `ElementTypeImpl`).
      Lambda with implementation configuration is optional.
      Note that this function returns an object of type `Implementation`.
    - `noImpl(element: Element)` used when you don't want to generate any implementation for `element`/
- In the configuration lambda you can:
    - Describe the kind of the implementation — `FinalClass` (default), `OpenClass`, `AbstractClass`, `Interface` using the syntax
      `kind = Interface`
    - Add parents for the implementation class
        - syntax: `parents += parent`.
        - `parent` can be only implementation with `kind = Interface`.
    - Configure default values for fields:
        - `default(fieldName: String) { ... }`
            - in configuration lambda you can describe:
                - `value = _defaultValue_`.
                - `withGetter = true/false` (`false` by default).
                - `delegate = delegateFieldName` (used for generating such fields: `val typeRef: FirTypeRef get() = expression.typeRef`).
                - `delegateName = fieldNameInDelegateType` (`val expressionTypeRef: FirTypeRef get() = expression.typeRef`).
                - `needAcceptAndTransform = true/false` (`true` by default) -- specify it if you don't want to accept field in
                  `acceptChildren`.
                - `customSetter = setterExpresison`.
            - note that by default, all fields with fir elements are mutable, and others are immutable.
        - Also, there are some aliases for that default:
            - `default(fieldName, value)`
            - `defaultNull(fieldName, [withGetter: Boolean])`
    - If some fields should be `lateinit`, you describe them in the `lateinit(vararg fields: String)` call.
    - If you use some types that should be imported, list them by calling `additionalImports(vararg types: Importable)`   

# Types

- Types commonly used in configuration are listed in [`Types.kt`](src/org/jetbrains/kotlin/fir/tree/generator/Types.kt)
- There are multiple ways to describe a new type:
    - `fun <reified T : Any> type()` uses FQN of the corresponding `T` class.
    - `fun type(packageName: String, typeName: String, exactPackage: Boolean = false, kind: TypeKind = TypeKind.Interface)`.
        - if `exactPackage = false`, its return type with default package prefix: `org.jetbrains.kotlin.packageName.typeName`.
        - otherwise, there is no default prefix: `packageName.typeName` .
    - `generatedType([packageName: String], typeName: String)` — same as `type(packageName, typeName)` but with
      the `org.jetbrains.kotlin.fir` prefix .

# Notes

- There is an algorithm that automatically makes as most abstract classes instead of interfaces as possible.
  If you want to some `Element` or `Implementation` should be always an interface you should:
    - call `kind = ImplementationKind.Interface` when configuring a `Element` in `FirTree.kt`
    - specify `kind = Interface` when configuring an `Implementation` in `ImplementationConfigurator.kt`
