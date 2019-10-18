# Fir elements

- All fir elements are listed in `FirTreeBuilder.kt`
- Syntax of new element declaration: `element(elementName, elementKind: Kind, vararg parents: Element)`
    - `elementName` is a name of declared element. If `elementName = Foo` then it's type will be `FirFoo`
    - kind describes target package for element. Avaliable kinds:
        - `Expression` (package `fir.expression`)
        - `Declaration` (package `fir.declaration`)
        - `Reference` (package `fir.references`)
        - `TypeRef` (package `fir.types`)
        - `Other` (package `fir`)
    - if no one parent element was not declaraed than generated element will be direct inheritor of `FirElement`

# Types

- All types, used in elements and their implementations are described with object of class `Type`
- `Type` objects are used for generating imports in generated files
- Types commonly used in configuration are listed in `Types.kt`
- There is multiple ways to describe new type:
    - `type(klass: KClass<*>)` uses FQN of corresponding class
    - `type(packageName: String, typeName: String, exactPackage: Boolean = false)`
        - if `exactPackage = false` it's return type with default package prefix: `org.jetbrains.kotlin.packageName.typeName`
        - otherwise there is no default prefix: `packageName.typeName` 
    - `type(typeName: String)` creates type with no package, used only for types of type parameters (**Do not use it directly**)
    - `generatedType([packageName: String], typeName: String)` -- same as `type(packageName, typeName)` but with `org.jetbrains.kotlin.fir` prefix 

# Content of elements

- Fields of elements are described in `NodeConfigurator.kt`
- Syntax: 
```
elementName.configure {
    // node configuration
}
```
- **Fields:**
    - `Field` class describes field of element
    - There is multiple ways of creating new fields, but they have similar syntax: `field(..., nullable: Boolean = false, withReplace: Boolean)`
        - if `isNullable` is true then field type will be nullable
        - if `withReplace` is true then in element will be generated method `replace...` for that field
        - in place of `...` you can pass optional name (with `String` type) and `Type` or `Element` object
        - if no `name` passed then it will be generated based on type
        - if `Type` or `Element` has type argumetns you want to specify then you can call method `Type.withArgs(vararg types: String)` or `Element.withArgs(vararg replacements: Pair<String, String>)`
    - Also you can create fields with lists of some types
        - Lists can holds only fir element
        - Syntax: `fieldList([name: String], element: Element)` (if name no specified it will be generated based on type of `element`)
    - And there are helper functions for fields of primitive types that takes name of field: `booleanField`, `intField`, `stringField` 
    - If you want generate `transform...` function for field you should call method `withTransform()` on it
    - To add field to configuring node you should call infix `+` operator: `+fieldList("catches", catchClause).withTransform()`
    - Also you can use method `symbol(symbolTypeName: String, [argument: String])` to create field named `symbol` with type lying in `org.jetbrains.kotlin.fir.symbols` package
    - Some predefined fields are listed in `FieldSets.kt`
- If your node has some `tansform...` methods and you want to add methods for transforming all other children you should call `needTransformOtherChildren()`
- If your element has type parameters you should declare them using method `withArg(typeParameterName: String, [upperBound: Type/Element])`
- If element inherits element with type parameters you should  match that parameters with concrete types using method `parentArg(parent: Element, typeParameterName: String, typeArgument: String/Type/Element)`
- Note that if some element contains type parameters it should be configured before it's inheritors (will be fixed later)

# Implementations

- If element has no inheritors then it will have default implementation. Otherwise you should declare implementation that you want
- All implementations are described in `ImplementationConfigurator.kt`
- Syntax:
    - `impl(element: Element, [name: String]) {...}` describes configuration of element with name `name` (if there is no name then it would be `ElementTypeImpl`). Lambda with implementation configuration is optional. Note that this function returns object of type `Implementation`
    - `noImpl(element: Element)` used when you don't want to generated implementation of `element`
- In configuration lambda you can:
    - Describe kind of implementation -- `FinalClass` (default), `OpenClass`, `AbstractClass`, `Interface` using syntax `kind = Interface`
    - Add parents for implementation class
        - syntax: `parents += parent`
        - `parent` can be only implementation with `kind = Interface`
    - Configure default values for fields:
        - `default(fieldName: String) { ... }`
            - in configuration lambda you can describe:
                - `value = _defaultValue_`
                - `withGetter = true/false` (`false` by default)
                - `delegate = delegateFieldName` (used for generating such fields: `val typeRef: FirTypeRef get() = expression.typeRef`)
                - `delegateName = fieldNameInDelegateType` (`val expressionTypeRef: FirTypeRef get() = expression.typeRef`)
                - `needAcceptAndTransform = true/false` (`true` by default) -- specify it if you don't want to accept field in `acceptChildren`
                - `customSetter = setterExpresison`
            - note that by default all fields with fir elements are mutable and others are immutable
        - Also there is some aliases for that default:
            - `default(fieldName, value)`
            - `defaultNull(fieldName, [withGetter: Boolean])`
    - If some fields should be `lateinit` you describe them in call `lateinit(vararg fields: String)`
    - If you use some types that shoub be imported list them in method `useTypes(vararg types: Type/Element)`   

# Notes

- There is algorithm that automatically makes as most abstract classes instead of interfaces as possible. If you want to some `Element` or `Implementation` should be always an interface you should:
    - call `shouldBeAnInterface` when configuring a `Element` in `NodeConfigurator.kt`
    - specify `kind = Interface` when configuring an `Implementation` in `ImplementationConfigurator.kt`