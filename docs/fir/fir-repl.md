# FIR REPL Snippets

## Conversion

Kotlin REPL snippets are converted to `object`s during compilation.
This is conversion is performed during parsing.
Declarations are extracted to exist at the class level while statements are moved to the body of a function: `$$eval`.
To make sure resolution happens in the correct order,
the body of the `$$eval` function is resolved first and each declaration is resolved as it originally appeared in the snippet.

### Snippet

```kotlin
// SNIPPET
data class Foo(val x: Int)
private fun Foo.plusOne() = Foo(x + 1)
val x = Foo(1)
println(x.plusOne())
```

### Transformed

```kotlin
object Snippet {
    fun `$$eval`() {
        // <jump to resolve Foo>
        // <jump to resolve plusOne>
        // <jump to resolve x>
        println(x.plusOne())
    }
    
    data class Foo(val x: Int)
    private fun Foo.plusOne() = Foo(x + 1)
    val x = Foo(1)
}
```

## Type Resolution

To resolve classifiers from previous snippets, the `FirReplSnippetResolveExtension` provides an `FirScope` for all previous snippet declarations.
This scope is added during `SUPER_TYPES`, `TYPES`, and `BODY_RESOLVE` phases.

## Body Resolution

To achieve the resolution jump from the `$$eval` function, a new `FirStatement` node was introduced called `FirReplDeclarationReference`.
This new node holds a symbol for the class-level declaration it references.
Declarations moved to the class-level are tagged with the `isReplSnippetDeclaration` attribute.

During resolution, when a `FirReplDeclarationReference` is encountered, the referenced declaration is immediately resolved.
By inserting a `FirReplDeclarationReference` node in the `$$eval` function body where each declaration originally appeared in the snippet,
this preserves the control-flow graph of the snippet.

It is imperative that the `$$eval` function be resolved first.
This is done naturally by the compiler by placing it as the first declaration in the transformed object.

### Control-Flow / Data-Flow

Changes to the control-flow graph are required to treat what are technically class-level properties as local properties for smart-casting.
Because of how body resolution jumps to each declaration, the control-flow graph is constructed correctly.
However, since properties inferred to anonymous types need to be approximated, this requires smart-casting after property initialization.

[//]: # (TODO - there probably more to be said here, but not sure what details are important)

### Candidates

It is possible to reference declarations defined in previous snippets.
It is also possible to override declarations from previous snippets with new declarations.

```kotlin
// SNIPPET
val x = 1
fun foo(): String = "foo"

// SNIPPET
val x = 2
fun foo(): String = "bar"

// SNIPPET
println(foo() + x)
```

`FirReplSnippetResolveExtension` is what provides access to an `FirScope` for all previous snippets.
This will walk the REPL history and add class-level declarations to a custom scope.
The `FirScope` for previous snippets is a separate tower level from the current snippet.
This means that declarations from the current snippet will always take precedence over declarations from previous snippets,
even if declarations from previous snippets have more specific types.

```kotlin
// SNIPPET
fun foo(x: Any): Int = 1
fun foo(x: Int): Int = 2

foo(0) // 2

// SNIPPET
fun foo(x: Number): Int = 3

foo(0) // 3
```

`ReplOverloadCallConflictResolver` is what limits callable candidates to only the most recent snippet.
This is what allows repeatedly defining declarations with the same name without conflicts.
Declarations from previous snippets are tagged with the `originalReplSnippetSymbol` attribute.

## Future: Suspend

In the future, to support suspend function calls in REPL snippets, property initializers will need to structurally exist within the `$$eval`
function body.
This will hopefully avoid any changes to FIR checkers which validation the ability to call a suspend function from the correct scope.

[//]: # (TODO)

### Snippet

```kotlin
// SNIPPET
data class Foo(val x: Int)
private suspend fun Foo.plusOne() = Foo(x + 1)
val x = Foo(1).plusOne()
println(x)
```

### Transformed

```kotlin
object Snippet {
    fun `$$eval`() {
        // <jump to resolve Foo>
        // <jump to resolve plusOne>
        x = Foo(1).plusOne() // <repl property initializer>
        println(x)
    }
    
    data class Foo(val x: Int)
    private suspend fun Foo.plusOne() = Foo(x + 1)
    val x // = <expression reference>
}
```