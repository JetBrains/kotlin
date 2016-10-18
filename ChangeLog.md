# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.1-M02 (EAP-2)

### Language features

+ **Destructuring for lambdas** ([proposal](https://github.com/Kotlin/KEEP/issues/32))

    Current limitations:

    - Nested destructuring is not supported
    - Destructuring in named functions/constructors is not supported
        
### Compiler

#### Smart cast enhancements
- [`KT-2127`](https://youtrack.jetbrains.com/issue/KT-2127) Smart cast receiver to not null after a not null safe call
- [`KT-6840`](https://youtrack.jetbrains.com/issue/KT-6840) Make data flow information the same for assigned and assignee
- [`KT-13426`](https://youtrack.jetbrains.com/issue/KT-13426) Fix exception when smartcast on both dispatch & extension receiver

#### Bound references related issues
- [`KT-12995`](https://youtrack.jetbrains.com/issue/KT-12995) Do not skip generation of the left-hand side for intrinsic bound references and class literals
- [`KT-13075`](https://youtrack.jetbrains.com/issue/KT-13075) Fix codegen for bound class reference
- [`KT-13110`](https://youtrack.jetbrains.com/issue/KT-13110) Fix type mismatch error on class literal with integer receiver expression
- [`KT-13172`](https://youtrack.jetbrains.com/issue/KT-13172) Report error on "this::class" in super constructor call
- [`KT-13271`](https://youtrack.jetbrains.com/issue/KT-13271) Fix incorrect unsupported error on synthetic extension call on LHS of ::
- [`KT-13367`](https://youtrack.jetbrains.com/issue/KT-13367) Inline bound callable reference if it's used only as a lambda

#### Coroutines related issues
- [`KT-13156`](https://youtrack.jetbrains.com/issue/KT-13156) Do not execute last Unit-typed coroutine statement twice
- [`KT-13246`](https://youtrack.jetbrains.com/issue/KT-13246) Fix VerifyError with coroutines on Dalvik
- [`KT-13289`](https://youtrack.jetbrains.com/issue/KT-13289) Fix VerifyError with coroutines: Bad type on operand stack
- [`KT-13409`](https://youtrack.jetbrains.com/issue/KT-13409) Fix generic variable spilling with coroutines
- [`KT-13531`](https://youtrack.jetbrains.com/issue/KT-13531) Fix ClassCastException when coercion to Unit interacts with generic await() and coroutines
- Prohibit `Continuation<*>` as a last parameter of suspend functions
- [`KT-13560`](https://youtrack.jetbrains.com/issue/KT-13560) Prohibit non-Unit suspend functions

#### Typealises related issues
- [`KT-13200`](https://youtrack.jetbrains.com/issue/KT-13200) Fix incorrect number of required type arguments reported on typealias
- [`KT-13181`](https://youtrack.jetbrains.com/issue/KT-13181) Fix unresolved reference for a type alias from a different module
- [`KT-13161`](https://youtrack.jetbrains.com/issue/KT-13161) Support java static methods calls with typealiases
- [`KT-13835`](https://youtrack.jetbrains.com/issue/KT-13835) Do not lose nullability information  while expanding type alias in projection position
- [`KT-13422`](https://youtrack.jetbrains.com/issue/KT-13422) Prohibit usage of type alias to exception class as an object in 'throw' expression 
- [`KT-13735`](https://youtrack.jetbrains.com/issue/KT-13735) Fix NoSuchMethodError for generic typealias access
- [`KT-13513`](https://youtrack.jetbrains.com/issue/KT-13513) Support SAM constructors for aliased java functional types
- [`KT-13822`](https://youtrack.jetbrains.com/issue/KT-13822) Fix exception for start-projection of a type alias
- [`KT-14071`](https://youtrack.jetbrains.com/issue/KT-14071) Prohibit using type alias as a qualifier for super
- [`KT-14282`](https://youtrack.jetbrains.com/issue/KT-14282) Report error on unused type alias with -language-version 1.0
- [`KT-14274`](https://youtrack.jetbrains.com/issue/KT-14274) Fix type alias resolution when it's used for supertype constructor call

#### JDK dependent built-in classes related issues
- [`KT-13209`](https://youtrack.jetbrains.com/issue/KT-13209) Change first parameter's type of Map.getOrDefault to K instead of Any
- [`KT-13069`](https://youtrack.jetbrains.com/issue/KT-13069) Do not emit invalid DefaultImpls delegation when interface extends MutableMap with JDK8

#### `data` classes and inheritance
- [`KT-11306`](https://youtrack.jetbrains.com/issue/KT-11306) Allow data classes to implement equals/hashCode/toString from base classes

#### Various JVM code generation issues
- [`KT-13182`](https://youtrack.jetbrains.com/issue/KT-13182) Fix compiler internal error at inline
- [`KT-13757`](https://youtrack.jetbrains.com/issue/KT-13757) Prohibit referencing nested classes by name with $
- [`KT-12985`](https://youtrack.jetbrains.com/issue/KT-12985) Do not create range instances for 'for' loop in CharSequence.indices
- [`KT-13931`](https://youtrack.jetbrains.com/issue/KT-13931) Optimize generated code for IntRange#contains

#### Various analysis & diagnostic issues
- [`KT-10001`](https://youtrack.jetbrains.com/issue/KT-10001) Fix false unnecessary non-null assertion on a pair element
- [`KT-12811`](https://youtrack.jetbrains.com/issue/KT-12811) DECLARATION_CANT_BE_INLINED on an overridden member of a final class
- [`KT-435`](https://youtrack.jetbrains.com/issue/KT-435) Use parameter names in error messages when calling a function-valued expression
- - [`KT-13961`](https://youtrack.jetbrains.com/issue/KT-13961) REDECLARATION not reported on private-in-file 'foo' vs public 'foo' in different file
### JS

#### Bugfixes
- [`KT-13544`](https://youtrack.jetbrains.com/issue/KT-13544) Support type aliases in JS
- [`KT-13345`](https://youtrack.jetbrains.com/issue/KT-13345) Support class literals in JS

#### Library updates
- [`KT-18`](https://youtrack.jetbrains.com/issue/KT-18) Move exceptions from `java.lang` to `kotlin` package
- [`KT-12386`](https://youtrack.jetbrains.com/issue/KT-12386) Rewrite JS collections in Kotlin, move them to `kotlin.collections` package
- [`KT-7809`](https://youtrack.jetbrains.com/issue/KT-7809) Make Collection implementations conform to their declared interfaces
- [`KT-7473`](https://youtrack.jetbrains.com/issue/KT-7473) Make AbstractCollection.equals check object type
- [`KT-13429`](https://youtrack.jetbrains.com/issue/KT-13429) Make 'remove' on fresh iterator throw exception  instead of removing last element
- [`KT-13459`](https://youtrack.jetbrains.com/issue/KT-13459) Make JS implementation of ArrayList::add(index, element) check the index is in valid range
- [`KT-8724`](https://youtrack.jetbrains.com/issue/KT-8724) Fix MutableIterator.remove() for HashMap
- [`KT-10786`](https://youtrack.jetbrains.com/issue/KT-10786) Make Map.keys return view of map keys instead of snapshot
- [`KT-14194`](https://youtrack.jetbrains.com/issue/KT-14194) Make HashMap.putAll implementation not to call getKey/getValue

### Standard Library

#### Backward compatibility
- [`KT-14297`](https://youtrack.jetbrains.com/issue/KT-14297) Add @SinceKotlin annotation to support compatibility with compilation against older standard library
- [`KT-14213`](https://youtrack.jetbrains.com/issue/KT-14213) Ensure printStackTrace can be called with -language-version 1.0

#### Enhancements
- [`KEEP-53`](https://github.com/Kotlin/KEEP/blob/master/proposals/stdlib/abstract-collections.md) Provide two distinct hierarchies of abstract collections: one for implementing read-only/immutable collections, and other for implementing mutable collections
- [`KEEP-13`](https://github.com/Kotlin/KEEP/blob/master/proposals/stdlib/map-copying.md) Provide extension functions to copy maps
- [`KT-18`](https://youtrack.jetbrains.com/issue/KT-18) Introduce type aliases for common exceptions from `java.lang` in `kotlin` package
- [`KT-12762`](https://youtrack.jetbrains.com/issue/KT-12762) Make `kotlin.ranges.until` return an empty range for "illegal" 'to' parameter
- [`KT-12894`](https://youtrack.jetbrains.com/issue/KT-12894) Allow nullable receiver for `use` extension

### Reflection

#### New features
- [`KT-8998`](https://youtrack.jetbrains.com/issue/KT-8998) Introduce comprehensive API to work with KType instances 
- [`KT-10447`](https://youtrack.jetbrains.com/issue/KT-10447) Provide a way to check if a KClass is a data class
- [`KT-11284`](https://youtrack.jetbrains.com/issue/KT-11284) Add KClass<T>.cast extension
- [`KT-13106`](https://youtrack.jetbrains.com/issue/KT-13106) Support annotation constructors in reflection

#### Optimizations
- [`KT-10651`](https://youtrack.jetbrains.com/issue/KT-10651) Optimize KClass.simpleName

### IDE

###### New features
- [`KT-12903`](https://youtrack.jetbrains.com/issue/KT-12903) Implement "Inline type alias" refactoring
- [`KT-12902`](https://youtrack.jetbrains.com/issue/KT-12902) Implement "Introduce type alias" refactoring
- [`KT-12904`](https://youtrack.jetbrains.com/issue/KT-12904) Implement "Create type alias from usage" quick fix
- [`KT-9016`](https://youtrack.jetbrains.com/issue/KT-9016) Make use of named higher order function parameters
- [`KT-12205`](https://youtrack.jetbrains.com/issue/KT-12205) Suggest import of Kotlin static members in editor with Java source
- [`KT-13941`](https://youtrack.jetbrains.com/issue/KT-13941) Implement intention for introducing destructured lambda parameters when it's possible
- [`KT-13943`](https://youtrack.jetbrains.com/issue/KT-13943) Implement inspection and quickfix for to detect a manual destructuring of for / lambda parameter

###### Issues fixed
- [`KT-13004`](https://youtrack.jetbrains.com/issue/KT-13004) Support bound method references in completion
- [`KT-13242`](https://youtrack.jetbrains.com/issue/KT-13242) Suggest 'typealias' keyword in completion
- [`KT-13244`](https://youtrack.jetbrains.com/issue/KT-13244) Override/Implement Members: Do not expand type aliases in the generated members
- [`KT-13611`](https://youtrack.jetbrains.com/issue/KT-13611) Go to Class: Fix presentation of type aliases
- [`KT-13759`](https://youtrack.jetbrains.com/issue/KT-13759) Rename: Process object-wrapping alias references
- [`KT-13955`](https://youtrack.jetbrains.com/issue/KT-13955) Find Usages: Add special type for usages inside of type aliases
- [`KT-13479`](https://youtrack.jetbrains.com/issue/KT-13479) Support navigation to type aliases from binaries
- [`KT-13766`](https://youtrack.jetbrains.com/issue/KT-13766) Fix optimize imports not to add wrong and unnecessary import because of type alias
- [`KT-12949`](https://youtrack.jetbrains.com/issue/KT-12949) Consider type aliases as candidates for import
- [`KT-13266`](https://youtrack.jetbrains.com/issue/KT-13266) Suggest non-imported type aliases in completion
- [`KT-13689`](https://youtrack.jetbrains.com/issue/KT-13689) Do not treat type alias constructor usage as original type usage for optimize imports

### Scripting

- A new library `kotlin-script-util` containing utilities for implementing kotlin script support  
- [`KT-7880`](https://youtrack.jetbrains.com/issue/KT-7880) Experimental support for JSR 223 Scripting API
- [`KT-13975`](https://youtrack.jetbrains.com/issue/KT-13975), [`KT-14264`](https://youtrack.jetbrains.com/issue/KT-14264) Convert error on retrieving gradle plugin settings to warning
- Implement support for custom template-based scripts in command-line compiler, maven and gradle plugins

### Previous releases

This release also includes the following issues fixed in [`1.0.5`](https://github.com/JetBrains/kotlin/blob/c3fb2bb2aa346f295663a781b4497b942da27cd9/ChangeLog.md)
and [`1.1-M01`](https://github.com/JetBrains/kotlin/blob/54f891ab30eab3b3f2da05094f088259ded62b96/ChangeLog.md)

Note that not all of the issues of the final 1.0.5 release will be included in M02, but only ones mentioned on the page from the hyperlink above.
