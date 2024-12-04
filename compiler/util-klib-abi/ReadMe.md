## API for extracting publicly visible ABI from KLIBs

This is the API to extract and dump declarations from KLIBs that comprise publicly visible part of KLIB ABI. Can be used for implementing various KLIB-oriented build tools that do ABI compatibility validation, perform compilation avoidance with KLIBs, etc.

There are two major entry points:
* [LibraryAbiReader](src/org/jetbrains/kotlin/library/abi/LibraryAbiReader.kt) - extracts publicly visible ABI. The result is a loosely coupled tree (see [LibraryAbi](src/org/jetbrains/kotlin/library/abi/LibraryAbi.kt)) where the roots are the top-level declarations.
* [LibraryAbiRenderer](src/org/jetbrains/kotlin/library/abi/LibraryAbiRenderer.kt) - renders publicly visible ABI to a human-readable textual representation.

### The rendering format

* A line per a declaration: `declaration_part` `//` `signature_part`
  * `declaration_part` includes declaration keyword, modifiers and all valuable information that makes sense for ABI compatibility of the declaration.
  * `signature_part` is just a textual representation of declaration's IR signature.
* Declarations are always sorted in the stable order. So, changes in program order or serialization order have no effect.
* As long as the [LibraryAbiRenderer](src/org/jetbrains/kotlin/library/abi/LibraryAbiRenderer.kt)'s output remains the same we can confidently say that the library is still ABI-compatible even if the source code was dramatically changed. 

Example:
* Original *.kt file:
```
package org.sample

class Foo<T>(var value: T?) where T : Appendable, T : List<Number> {
    inline fun f(
        i: Int,
        s: String? = "hello",
        t: T,
        b1: () -> Unit,
        noinline b2: (Int) -> String,
        crossinline b3: String?.() -> Int?,
        vararg x: Any?
    ): Map<Long, Double>
}
```
* The rendered text:
```
final class <#A: kotlin.collections/List<kotlin/Number> & kotlin.text/Appendable> org.sample/Foo { // org.sample/Foo|null[0]
    final var value // org.sample/Foo.value|{}value[0]
        final fun <get-value>(): #A? // org.sample/Foo.value.<get-value>|<get-value>(){}[0]
        final fun <set-value>(#A?) // org.sample/Foo.value.<set-value>|<set-value>(1:0?){}[0]
    constructor <init>(#A?) // org.sample/Foo.<init>|<init>(1:0?){}[0]
    final inline fun f(kotlin/Int, kotlin/String? =..., #A?, kotlin/Function0<kotlin/Unit>, noinline kotlin/Function1<kotlin/Int, kotlin/String>, crossinline kotlin/Function1<kotlin/String?, kotlin/Int?>, kotlin/Array<out kotlin/Any?>...): kotlin.collections/Map<kotlin/Long, #A> // org.sample/Foo.f|f(kotlin.Int;kotlin.String?;1:0?;kotlin.Function0<kotlin.Unit>;kotlin.Function1<kotlin.Int,kotlin.String>;kotlin.Function1<kotlin.String?,kotlin.Int?>;kotlin.Array<out|kotlin.Any?>...){}[0]
}
```