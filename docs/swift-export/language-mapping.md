# Kotlin to Swift mapping

This document gives a quick superficial overview on how Swift export translates Kotlin declarations to Swift.
Currently, Swift export supports only a subset of the Kotlin language; thus this list is intentionally incomplete.

> ❗️ Swift export is in very early stages of development.
> Things change very quickly, and documentation might not catch up at times.
> 
> Please note that this part of Swift export will change **very often**.
> Some features are currently implemented the way they are not because this is what we consider _the best_,
> but because it’s easier to start with _some_ (even ugly) quick restricted solutions first, and then gradually improve it towards something
> beautiful.

# Declarations

## Classifiers

### Classes

Swift export supports only final classes that directly inherit from `Any` (like `class Foo()`). 
They are translated to Swift classes that inherit from a special `KotlinBase` class.
```kotlin
class MyClass {
    val property: Int = 0
    
    fun method() {}
}
```
```swift
public class MyClass : KotlinRuntime.KotlinBase {
    public var property: Swift.Int32 {
        get {
            ...
        }
    }
    public override init() {
        ...
    }
    public func method() -> Swift.Void {
        ...
    }
}
```

### Objects

Objects are translated to Swift classes with private `init` and static `shared` accessor.
```kotlin
object O
```
```swift
public class O : KotlinRuntime.KotlinBase {
    public static var shared: O {
        get {
            ...
        }
    }
    private override init() {
        ...
    }
}
```

### Typealiases

Typealiases are exported "as is":
```kotlin
typealias MyInt = Int
```

```swift
public typealias MyInt = Swift.Int32
```

## Callables

### Functions

Swift export supports only simple top-level functions and methods: no `suspend`, `inline`, `operator` or extensions.

```kotlin
fun foo(a: Short, b: Bar) {}

fun baz(): Long = 0
```

```swift
public func foo(a: Swift.Int16, b: Bar) -> Swift.Void {
    ...
}

public func baz() -> Swift.Int64 {
    ...
}
```

### Properties

Kotlin properties are translated to Swift properties.

```kotlin
val a: Int = 0
            
var b: Short = 15

const val c: Int = 0
```

```swift
public var a: Swift.Int32 {
    get {
        ...
    }
}
public var b: Swift.Int16 {
    get {
        ...
    }
    set {
        ...
    }
}
public var c: Swift.Int32 {
    get {
        ...
    }
}
```

### Constructors

Constructors are translated to Swift initializers.

```kotlin
class Foo(val prop: Int)
```

```swift
public class Foo : KotlinRuntime.KotlinBase {
    public init(
        prop: Swift.Int32
    ) {
        ...
    }
}
```

# Types

## Built-in types

### Primitive types

| Kotlin  | Swift                  |
|---------|------------------------|
| Boolean | Bool                   |
| Char    | Unicode.UTF16.CodeUnit |
| Byte    | Int8                   |
| Short   | Int16                  |
| Int     | Int32                  |
| Long    | Int64                  |
| UByte   | UInt8                  |
| UShort  | UInt16                 |
| UInt    | UInt32                 |
| ULong   | UInt64                 |
| Float   | Float                  |
| Double  | Double                 |

### kotlin.Any

`Any` is translated to the special `KotlinBase` class.

### kotlin.Unit

`Unit` is translated to the `Void` type.

### kotlin.Nothing

`Nothing` is translated to the `Never` type.

```kotlin
fun foo(): Nothing = TODO()

fun baz(input: Nothing) {}
```

```swift
public func foo() -> Swift.Never {
    ...
}

public func baz(input: Swift.Never) -> Void {
    ...
}
```

## Classifier types

Swift export supports only a limited number of reference types for now: final classes that directly inherit from `Any`.

# Namespaces

## Packages

Kotlin packages are translated to nested Swift enums to avoid name collisions.
```kotlin
// FILE: bar.kt
package foo.bar

fun callMeMaybe() {}

// FILE: baz.kt
package foo.baz

fun callMeMaybe() {}
```

```swift
public extension foo.bar {
    public func callMeMaybe() {}
}

public extension foo.baz {
    public func callMeMaybe() {}
}

public enum foo {
    public enum bar {}
    
    public enum baz {}
}
```



