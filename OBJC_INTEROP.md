# _Kotlin/Native_ interoperability with Swift/Objective-C

This documents covers some details of Kotlin/Native interoperability with
Swift/Objective-C.

## Usage

Kotlin/Native provides bidirectional interoperability with Objective-C.
Objective-C frameworks and libraries can be used in Kotlin code if
properly imported to the build (system frameworks are imported by default).
See e.g. "Interop libraries" in
[Gradle plugin documentation](GRADLE_PLUGIN.md#building-artifacts).
Swift library can be used in Kotlin code if its API is exported to Objective-C
with `@objc`. Pure Swift modules are not yet supported.

Kotlin module can be used in Swift/Objective-C code if compiled into a
[framework](GRADLE_PLUGIN.md#framework). See [calculator sample](samples/calculator)
as an example.

## Mappings

The table below shows how Kotlin concepts are mapped to Swift/Objective-C and vice versa.

| Kotlin | Swift | Objective-C | Notes |
| ------ | ----- |------------ | ----- |
| `class` | `class` | `@interface` | [note](#name-translation) |
| `interface` | `protocol` | `@protocol` | |
| `constructor`/`create` | Initializer | Initializer | [note](#initializers) |
| Property | Property | Property | [note](#top-level-functions-and-properties) |
| Method | Method | Method | [note](#top-level-functions-and-properties) [note](#method-names-translation) |
| `@Throws` | `throws` | `error:(NSError**)error` | [note](#errors-and-exceptions) |
| Extension | Extension | Category member | [note](#category-members) |
| `companion` member <- | Class method or property | Class method or property |  |
| `null` | `nil` | `nil` | |
| `Singleton` | `Singleton()`  | `[Singleton singleton]` | [note](#kotlin-singletons) |
| Primitive type | Primitive type / `NSNumber` | | [note](#nsnumber) |
| `Unit` return type | `Void` | `void` | |
| `String` | `String` | `NSString` | |
| `String` | `NSMutableString` | `NSMutableString` | [note](#nsmutablestring) |
| `List` | `Array` | `NSArray` | |
| `MutableList` | `NSMutableArray` | `NSMutableArray` | |
| `Set` | `Set` | `NSSet` | |
| `MutableSet` | `NSMutableSet` | `NSMutableSet` | [note](#collections) |
| `Map` | `Dictionary` | `NSDictionary` | |
| `MutableMap` | `NSMutableDictionary` | `NSMutableDictionary` | [note](#mutable-collections) |
| Function type | Function type | Block pointer type | [note](#function-types) |

### Name translation

Objective-C classes are imported into Kotlin with their original names.
Protocols are imported as interfaces with `Protocol` name suffix,
i.e. `@protocol Foo` -> `interface FooProtocol`.
These classes and interfaces are placed into package [specified in build configuration](#usage)
(`platform.*` packages for preconfigured system frameworks).

Names of Kotlin classes and interfaces are prefixed when imported to Swift/Objective-C.
The prefix is derived from the framework name.

### Initializers

Swift/Objective-C initializers are imported to Kotlin as constructors and factory methods
named `create`. The latter happens with initializers declared in Objective-C category or
as Swift extension, because Kotlin has no concept of extension constructors.

Kotlin constructors are imported as initializers to Swift/Objective-C. 

### Top-level functions and properties

Top-level Kotlin functions and properties are accessible as members of a special class.
Each Kotlin package is translated into such a class. E.g.
```
package my.library

fun foo() {}
```

can be called from Swift like
```
Framework.foo()
```

### Method names translation

Generally Swift argument labels and Objective-C selector pieces are mapped to Kotlin
parameter names. Anyway these two concepts have different semantics, so sometimes
Swift/Objective-C methods can be imported with clashing Kotlin signature. In this case
clashing methods can be called from Kotlin using named arguments, e.g.:
```
[player moveTo:LEFT byMeters:17]
[player moveTo:UP byInches:42]
```

in Kotlin would be:

```
player.moveTo(LEFT, byMeters = 17)
player.moveTo(UP, byInches = 42)
```

### Errors and exceptions

Kotlin has no concept of checked exceptions, all Kotlin exceptions are unchecked.
Swift has only checked errors. So if Swift or Objective-C code calls Kotlin method
which throws an exception to be handled, then Kotlin method should be marked
with `@Throws` annotation. In this case all Kotlin exceptions
(except for instances of `Error`, `RuntimeException` and subclasses) are translated to
Swift error/`NSError`.

Note that the opposite translation is not implemented yet:
Swift/Objective-C error-throwing methods aren't imported to Kotlin as
exception-throwing.

### Category members

Members of Objective-C categories and Swift extensions are imported to Kotlin
as extensions. That's why these declarations can't be overridden in Kotlin.
And extension initializers aren't available as Kotlin constructors.

### Kotlin singletons

Kotlin singleton (made with `object` declaration, including `companion object`)
is imported to Swift/Objective-C as class with a single instance.
The instance is available through the factory method, i.e. as
`[MySingleton mySingleton]` in Objective-C and `MySingleton()` in Swift.

### NSNumber

While Kotlin primitive types in some cases are mapped to `NSNumber`
(e.g. when they are boxed), `NSNumber` type is not automatically translated 
to Kotlin primitive types when used as Swift/Objective-C parameter type or return value.
The reason is that `NSNumber` type doesn't provide enough information
about wrapped primitive value type, i.e. `NSNumber` is statically not known
to be e.g. `Byte`, `Boolean` or `Double`. So Kotlin primitive values 
should be cast to/from `NSNumber` manually (see [below](#casting-between-mapped-types)).

### NSMutableString

`NSMutableString` Objective-C class is not available from Kotlin.
All instances of `NSMutableString` are copied when passed to Kotlin.

### Collections

Kotlin collections are converted to Swift/Objective-C collections as described
by the table above. Swift/Objective-C collections are mapped to Kotlin in the same way,
except for `NSMutableSet` and `NSMutableDictionary`. `NSMutableSet` isn't converted to
Kotlin `MutableSet`. To pass an object for Kotlin `MutableSet`,
one can create this kind of Kotlin collection explicitly by either creating it 
in Kotlin with e.g. `mutableSetOf()`, or using `${prefix}MutableSet` class in
Swift/Objective-C, where `prefix` is the framework names prefix.
The same holds for `MutableMap`.

### Function types

Kotlin function-typed objects (e.g. lambdas) are converted to 
Swift functions / Objective-C blocks. However there is a difference in how
types of parameters and return values are mapped when translating a function
and a function type. In the latter case primitive types are mapped to their
boxed representation, `NSNumber`. Kotlin `Unit` return value is represented
as corresponding `Unit` singleton in Swift/Objective-C. The value of this singleton
can be retrieved in the same way as for any other Kotlin `object`
(see singletons in the table above).
To sum the things up:
```
fun foo(block: (Int) -> Unit) { ... }
```

would be represented in Swift as

```
func foo(block: (NSNumber) -> KotlinUnit)
```

and can be called like
```
foo {
    bar($0 as! Int32)
    return KotlinUnit()
}
```

## Casting between mapped types

When writing Kotlin code, an object may require to be converted from Kotlin type
to equivalent Swift/Objective-C type (or vice versa). In this case plain old
Kotlin cast can be used, e.g.
```
val nsArray = listOf(1, 2, 3) as NSArray
val string = nsString as String
val nsNumber = 42 as NSNumber
```

## Subclassing

### Subclassing Kotlin classes and interfaces from Swift/Objective-C

Kotlin classes and interfaces can be subclassed by Swift/Objective-C classes
and protocols.
Currently a class that adopts Kotlin protocol should inherit `NSObject`
(either directly or indirectly). Note that all Kotlin classes do inherit `NSObject`,
so a Swift/Objective-C subclass of Kotlin class can adopt Kotlin protocol.

### Subclassing Swift/Objective-C classes and protocols from Kotlin

Swift/Objective-C classes and protocols can be subclassed with Kotlin `final` class.
Non-`final` Kotlin classes inherting Swift/Objective-C types aren't supported yet, so it is
not possible to declare a complex class hierarchy inherting Swift/Objective-C types.

Normal methods can be overridden using `override` Kotlin keyword. In this case
overriding method must have the same parameter names as the overridden one.

Sometimes it is required to override initializers, e.g. when subclassing `UIViewController`. 
Initializers imported as Kotlin constructors can be overridden by Kotlin constructors
marked with `@OverrideInit` annotation:
```
class ViewController : UIViewController {
    @OverrideInit constructor(coder: NSCoder) : super(coder)

    ...
}
```
The overriding constructor must have the same parameter names and types as the overridden one.

To override different methods with clashing Kotlin signatures, one can add
`@Suppress("CONFLICTING_OVERLOADS")` annotation to the class.

By default Kotlin/Native compiler doesn't allow to call non-designated
Objective-C initializer as `super(...)` constructor. This behaviour can be
inconvenient if designated initializers aren't marked properly in the Objective-C
library. Adding `disableDesignatedInitializerChecks = true` to `.def` file for
this library would disable these compiler checks.

## C features

See [INTEROP.md](INTEROP.md) for the case when library uses some plain C features
(e.g. unsafe pointers, structs etc.).
