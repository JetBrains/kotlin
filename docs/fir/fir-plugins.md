# FIR Plugin API

Right now, FIR provides five different extensions, which may be used for different purposes. Most of them use a special predicate-based API for accessing declarations, so firstly there will be an explanation of those predicates and only then each extension point will be explained

## Annotations and predicates

Generally, FIR compiler supports search for declarations only with a specific classId/callableId. On the other hand, plugins usually want to perform some global lookup, because they don't know actual names of declarations in user code. To solve this problem, FIR plugin API provides a way to quickly lookup for specific declarations in the user code based on some specific predicate. Right now, the only way to communicate between user code and plugin is to mark something in code with some annotation, so the plugin will look at this annotation and do its magic. And for that, FIR provides a predicate API.

_Note:_ there are plans to design some new syntax for passing information from code to plugins instead of annotations, because they have some problems and limitations due to the compiler design reasons.

There is a special service in FIR named [FirPredicateBasedProvider](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/tree/src/org/jetbrains/kotlin/fir/extensions/FirPredicateBasedProvider.kt). It allows to find all declarations in compiled code which match some [DeclarationPredicate](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/tree/src/org/jetbrains/kotlin/fir/extensions/predicate/DeclarationPredicate.kt).

There are multiple types of predicates and each one has DSL functions to create them (in parenthesis):
- `AnnotatedWith` matches all declarations which have on of annotations passed to it (`has(vararg annotations: FqName)`)
- `UnderAnnotatedWith` matches all declarations which are declared inside class, which is annotated with on of annotations passed to it (`under(vararg annotations: FqName)`)
- `AnnotatedWithMeta` and `UnderMetaAnnotated` -- same but for meta annotations (`metaHas(vararg annotations: FqName)` and `metaUnder(vararg annotations: FqName)`)
- `And` and `Or` are predicates for combining other types of predicates

There are also functions `hasOrUnder` and `metaHasOrUnder` as shortcuts for `has(...) or under(...)` and `metaHas(...) or metaUnder(...)`

Those predicates take fully qualified names of annotation classes and those annotations must be part of the plugin (shipped with it).

_Meta annotations_ are annotations which users can use to mark their own annotations and then use them to mark declarations.

### Example

```kotlin
// my-super-plugin-annotations.jar
package my.super.plugin

annotation class MyAnnotation
annotation class MyMetaAnnotation

// user code
package test

import my.super.plugin.*

@MyMetaAnnotation
annotation class UserAnnotation

@MyAnnotation
class A {
    fun foo() {}
}

@UserAnnotation
class B {
    fun foo() {}
}

// my-super-plugin code
...
val provider = session.predicateBasedProvider
val myAnn = "my.super.plugin.MyAnnotation".toFqn()
val myMeta = "my.super.plugin.MyMetaAnnotation".toFqn()

provider.getSymbolsByPredicate(has(myAnn)) // [test.A]
provider.getSymbolsByPredicate(under(myAnn)) // [test.A.foo]
provider.getSymbolsByPredicate(hasOrUnder(myAnn)) // [test.A, test.A.foo]

provider.getSymbolsByPredicate(has(myMeta)) // [test.B]
provider.getSymbolsByPredicate(under(myMeta)) // [test.B.foo]
provider.getSymbolsByPredicate(hasOrUnder(myMeta)) // [test.B, test.B.foo]
```

**Important:** if you want to use some predicates in your plugin then you need to explicitly declare them in the method `FirExtension.registerPredicates`. If you don't do this then there is no guarantee that predicate based provider will find annotated declarations even they match with predicate. This limitation exists because predicate based provider builds an index of declarations based on all registered predicates at `ANNOTATIONS_FOR_PLUGINS`, which allows it to lookup for declarations with specific predicate (which was already indexed) for almost `O(1)`.

Also, there are two limitations about annotations which can be used for plugins:
1. Only top-level annotations can be used in predicates. This limitation exists because plugin annotations are resolved before `SUPERTYPES` stage, so compiler won't be able to resolve access to annotation in some class if it is defined as nested annotation of it's supertype
```kotlin
open class Base {
    annotation class Ann
} 

class Derived : Base() { // supertype Base not resolved yet
    @Ann // can not be resolved at plugin annotation phase
    fun foo() {}
}
```
2. Despite that plugin annotations are resolved in very early stage, their arguments will be resolved only on `ARGUMENTS_OF_ANNOTATIONS` phase, so you can not rely on the fact that all arguments are resolved in some extensions, which work before `ARGUMENTS_OF_ANNOTATIONS` phase.

## Extensions

All extensions to FIR compiler are inheritors of [FirExtension](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/tree/src/org/jetbrains/kotlin/fir/extensions/FirExtension.kt). It has only one  method, which can be overridden in custom extensions (`registerPredicates`) which was explained before.

For registering FIR extension you need to implement and register just one extension point named [FirExtensionRegistrar](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/entrypoint/src/org/jetbrains/kotlin/fir/extensions/FirExtensionRegistrar.kt?tab=source&line=24). It has one method to implement (`configurePlugin`) in which you need register all your FIR extensions, using special DSL syntax:

```kotlin
class MySuperFirRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        // there is a unaryPlus function on callable reference to extension constructor
        //   which registers specific extension
        +::MyFirstFirExtension
        +::MySecondFirExtension
        +::MyFirCheckersExtension
    }
}
```

This extension point can be registered in compiler like all other existing extension points, using `-Xplugin` CLI argument or via `META-INF.services`.

### FirSupertypeGenerationExtension

```kotlin
abstract class FirSupertypeGenerationExtension(session: FirSession) : FirExtension(session) {
    abstract fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean

    abstract fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>
    ): List<FirResolvedTypeRef>
}
```

[FirSupertypeGenerationExtension](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/extensions/FirSupertypeGenerationExtension.kt) is an extension which allows you to add additional super types to classes and interfaces. This extension is called at `SUPERTYPES` stage right after types of some class (`classLikeDeclaration`) are resolved (`resolvedSupertypes`) but not written to class itself yet. Note that you can not modify explicitly declared classes, only add new one.

For example, if `computeAdditionalSupertypes` returned some `[C, D]` list for class `Base : A, B`, then class `Base` will have four supertypes: `Base : A, B, C, D`.

Also note that `computeAdditionalSupertypes` will be called only if `needTransformSupertypes` returned `true` for specific class.

### FirStatusTransformerExtension


```kotlin
abstract class FirStatusTransformerExtension(session: FirSession) : FirExtension(session) {
    abstract fun needTransformStatus(declaration: FirDeclaration): Boolean

    open fun transformStatus(
        status: FirDeclarationStatus,
        declaration: FirDeclaration
    ): FirDeclarationStatus {
        return status
    }
    
    ...
    // overrides of `transformStatus` for different types of declarations
    ...
}
```

[FirStatusTransformerExtension](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/extensions/FirStatusTransformerExtension.kt) allows you to transform declaration status (visibility, modality, modifiers) for any non-local declaration. This extension called during `STATUS` phase right before inference of actual status from overrides. In `transformStatus` you may return new status with, for example, changed default modality. `transformStatus` will be called only if `needTransformStatus` returns `true` for specific declaration.

**Example:**
```kotlin
/*
 * transformStatus(function, status) {
 *   val newVisibility = if (status.visibility == Unknown) Public else status.visibility
 *   return status.withVisibility(newVisbility)
 * }
 */

abstract class Base {
    protected abstract fun foo()
}

@AllMembersPublic
class Derived : Base() {
    // without plugin `foo` is `protected` by default, 
    override fun foo() {}
}
/*
 * Status of Derived.foo before resolution:
 *   (visiblity = Unknown, modality = null, isOverride = true)
 * Status after resolution without plugin:
 *   (visiblity = Protected, modality = Final, isOverride = true)
 *
 * Status before resolution after plugin transformation:
 *   (visiblity = Public, modality = null, isOverride = true)
 * Status after resolution with plugin:
 *   (visiblity = Public, modality = Final, isOverride = true)
 */
```

**Important:** don't change visibility of classifiers (classes, objects, typealiases). This is not supported (and won't be), and in future this won't be allowed by API itself or at least using assertions

### FirDeclarationGenerationExtension

```kotlin
abstract class FirDeclarationGenerationExtension(session: FirSession) : FirExtension(session) {
    // Can be called on SUPERTYPES stage
    open fun generateClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? = null

    // Can be called on STATUS stage
    open fun generateFunctions(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirNamedFunctionSymbol> = emptyList()
    open fun generateProperties(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirPropertySymbol> = emptyList()
    open fun generateConstructors(owner: FirClassSymbol<*>): List<FirConstructorSymbol> = emptyList()

    // Can be called on IMPORTS stage
    open fun hasPackage(packageFqName: FqName): Boolean = false
  
    // Can be called after SUPERTYPES stage
    open fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> = emptySet()
    open fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>): Set<Name> = emptySet()
    open fun getTopLevelCallableIds(): Set<CallableId> = emptySet()
    open fun getTopLevelClassIds(): Set<ClassId> = emptySet()
}
```

[FirDeclarationGenerationExtension](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/providers/src/org/jetbrains/kotlin/fir/extensions/FirDeclarationGenerationExtension.kt) is an extension for generating new declarations (classes, functions, properties). Unlike `SyntheticResolveExtension`, which generated all members and nested classes for specific class at once, `FirDeclarationGenerationExtension` have provider-like API: compiler came to extensions with some specific classId or callableId and plugin generates declaration(s) with this ID if it is needed.

**Contracts and usage:**
- `generate...` functions will be called only if `get...Names/get...Ids` already returned corresponding ID.
- Results of all functions are cached, so it's guaranteed that each function in extension with specific arguments will be called only once
- Different methods of extensions can be called for the first time on different compiler stages (see comments), so be careful with that. This means that, for example, if you observe some class in `generateClassLikeDeclaration` then this class can have unresolved super types
- All declarations you return in `generate...` methods should be fully resolved: `status` should be `FirResolvedDeclarationStatus`, all type refs should be `FirResolvedTypeRef`, `resolvePhase` field should be set to `FirResolvePhase.BODY_RESOLVE`
- There is no need to generate body for functions and initializers for properties. They all can be filled in backed IR via `IrGenerationExtension`
- If you generate some class using `generateClassLikeDeclaration` then you don't need to fill it's `declarations`. Instead of that you need to generate members via `generateProperties/Functions/Constructors` methods of **same** generation extension (this is important note if you have multiple generation extensions in your plugin)
- If you want to generate constructor in class (from source or generated) then you need to add `SpecialNames.INIT` in `getCallableNamesForClass` for this class
- If you want to generate companion object in some class (with classId `outerClassId`) then you need to return `outerClassId.Companion` class id from `generateClassLikeDeclaration` for this class
- All generated declarations will be automatically converted to backend IR, so there is no need to manually generate IR declarations and replace references in whole module. All you need is just fill bodies of generated declarations
- For generated declarations you need to set special `origin` named `FirDeclarationOrigin.Plugin`. This origin takes object `key: FirPluginKey`, which will be saved in `IrPluginDeclarationOrigin` in IR for generated declaration, so you can use that key to pass data from frontend to backend (_there are plans to migrate this mechanism to IR declaration attributes, but right now they don't exist_)

### FirAdditionalCheckersExtension

```kotlin
abstract class FirAdditionalCheckersExtension(session: FirSession) : FirExtension(session) {
    open val declarationCheckers: DeclarationCheckers = DeclarationCheckers.EMPTY
    open val expressionCheckers: ExpressionCheckers = ExpressionCheckers.EMPTY
    open val typeCheckers: TypeCheckers = TypeCheckers.EMPTY
}
```

[FirAdditionalCheckersExtension](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/checkers/src/org/jetbrains/kotlin/fir/analysis/extensions/FirAdditionalCheckersExtension.kt) is used to enable some additional checkers which can report diagnostics. There are three main kinds of checkers (for declarations, expressions and types) and multiple types of checkers for each specific type of declaration/expression/typeRef in every kind. All you need is just declare all those checkers inside extension

### FirTypeAttributeExtension

_WIP_

### Other extensions

There are plans to implement some more extensions which allow to
- declare new kinds of contracts
- modify bodies of functions
    - change return types and resolved references of function calls (and other resolvable entities)
    - replace some expressions with new ones

# IDE integration

The whole FIR plugin API is designed in a way which provides IDE supports for plugins out of box, so there won't be any need to right separate IDE plugin for each compiler plugin. IDE integration right now in active development and shows very impressive results, but it is not ready for preview right now.

# Examples

- [fir-plugin-prototype](https://github.com/JetBrains/kotlin/tree/master/plugins/fir-plugin-prototype) sandbox plugin which tests all existing extension points
- [fir-parcelize](https://github.com/JetBrains/kotlin/tree/master/plugins/parcelize/parcelize-compiler/parcelize.k2/src/org/jetbrains/kotlin/parcelize/fir) FIR implementation of Parcelize plugin
