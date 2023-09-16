# K2 REPL design proposal

This document describes the in-detail design of the REPL compiler in K2.


## Preliminary knowledge

Previous documents:

https://docs.google.com/document/d/1giu97BIMOGryg9_SQtOyHHL5td7mv7UXV7DbBbFzt_0/edit#heading=h.jbfis5ki3dlk
https://docs.google.com/document/d/1XU8LgeExGXR6q0TMAt3ttS9ZdjGQ92rZYjoL1dYllj4/edit?pli=1



**Snippet** is a script-like Kotlin code fragment that can be executed inside the **REPL session**. Snippets can only be analyzed and executed inside the corresponding session. Snippets can be executed only sequentially, the order of analysis doesn’t matter. The sequence of already executed snippets inside the session forms the **REPL session state**. The state is divided into compile-time and runtime parts. Compile-time part is used during snippet compilation, runtime part is used when the compiled snippet is executed.


## Compilation Process

User wants to execute two snippets:

#1
```kotlin
val a = 1
val b = 2
fun f(k: Int) = k.toString()
a + b
```

#2
```kotlin
val a = "123"
a + f(b)
```

REPL initializes empty state. It also initializes compiler environment and FIR session.
All these are living until the end of the session.

### 1: Source -> PSI

First snippet is parsed into PSI the same way scripts are parsed:
```
<KtFile>
    <Preamble></Preamble> // Imports and file annotations go there
    <KtScript>
        <block>
            <statement>val a = 1</statement>
            <statement>val b = 2</statement>
            <statement>fun f(k: Int) = k.toString()</statement>
            <statement>a + b</statement>
        </block>
    </KtScript>
</KtFile>
```

### 2: PSI -> Raw FIR

Then, PSI is converted into raw FIR. **Each statement is converted into separate snippet.**
Reasons for it are described below.
Psi2Fir conversion also adds package directive to the containing file.

```
<FirFile>
    <Preamble>
        package myrepl.snippet1
    </Preamble>
    <FirSnippet>
        <statement>val a = 1</statement>
    </FirSnippet>
    <FirSnippet>
        <statement>val b = 2</statement>
    </FirSnippet>
    <FirSnippet>
        <statement>fun f(k: Int) = k.toString()</statement>
    </FirSnippet>
    <FirSnippet>
        <statement>a + b</statement>
    </FirSnippet>
</FirFile>
```

### 3: Raw FIR postprocessing

Then, FIR tree is converted into multiple files: each file has common preamble and a single snippet inside

```
<FirFile>
    <Preamble>
        package myrepl.snippet1
    </Preamble>
    <FirSnippet>
        <statement>val a = 1</statement>
    </FirSnippet>
</FirFile>
```

```
<FirFile>
    <Preamble>
        package myrepl.snippet1
    </Preamble>
    <FirSnippet>
        <statement>val b = 2</statement>
    </FirSnippet>
</FirFile>
```
(the rest 2 snippets are extracted in the same way)

All subsequent phases of compilation are performed separately for each of the extracted `FirFile`s.

### 4: FIR Resolve

We run the FIR transformations that are done the same way they are done for the
regular files. However, we need to add special scopes for resolving symbols from the
*REPL state*. We do it on the body resolve phase. We add a FIR session extension that holds the state
and constructs the scopes.

NB! We can only have one top-level declaration inside the snippet, see (3).

#### Callables and classes resolve
Callables and classes are already inside the FIR session, fully resolved.
However, they are each inside its own package. We can't import all the packages because we
will get the ambiguity for any declarations repeated more than once. We also can't import selected
packages because we may have several declarations inside each snippet package. That's why we create
special scope for callables and classes resolution. Inside these scopes, we search for the symbols
in all snippets packages in reversed direction. The list of packages is stored inside the REPL state.

#### Properties resolve
We could do the same thing for properties, but we suppose that properties' types could be
enhanced after the evaluation. That's why we store property names along with their types (TODO: types of these types)
inside the REPL state, and also provide them in the special resolution scope.
(TODO: some reasoning is missing here)

### 5: Resolve results collection

After the body resolve, we store all the resolved top-level variables
(there could be several variables in case of destructing assignment, also there could
be a result property in case if it's the last sub-snippet that contains expression-statement)
inside the REPL state to reuse them later

### 6: Checkers

After the resolve we should run FIR checkers

#### Control flow analysis

(TODO) We probably should unite all the snippets together again to do build correct control flow graph.
In particular, we need it for cases like this:

```kotlin
val x: Any = 1
x as Int
x + 1
```

### 7: FIR -> IR

### 8: IR lowerings

### 9: IR -> Bytecode
