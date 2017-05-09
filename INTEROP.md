# _Kotlin/Native_ interoperability #

## Introduction ##

 _Kotlin/Native_ follows general tradition of Kotlin to provide excellent
existing platform software interoperability. In case of native platform
most important interoperability target is a C library. Thus _Kotlin/Native_
comes with an `cinterop` tool, which could be used to quickly generate
everything needed to interact with an external library.

 Following workflow is expected when interacting with the native library.
   * create `.def` file describing what to include into bindings
   * use `cinterop` tool to produce Kotlin bindings
   * run _Kotlin/Native_ compiler on an application to produce the final executable

 Interoperability tool analyses C headers and produces "natural" mapping of
types, function and constants into the Kotlin world. Generated stubs can be
imported into an IDE for purposes of code completion and navigation.

## Simple example ##

Build the dependencies and the compiler (see `README.md`).

Prepare stubs for the system sockets library:

    cd samples/socket
    ../../dist/bin/cinterop -def sockets.def -o sockets.kt.bc

Compile the echo server:

    ../../dist/bin/kotlinc EchoServer.kt -library sockets.kt.bc \
     -o EchoServer.kexe

This whole process is automated in `build.sh` script, which also support cross-compilation
to supported cross-targets with `TARGET=raspberrypi ./build.sh` (`cross_dist` target must
be executed first).

Run the server:

    ./EchoServer.kexe 3000 &

Test the server by conecting to it, for example with telnet:

    telnet localhost 3000

Write something to console and watch server echoing it back.

## Creating bindings for a new library ##

 To create bindings for a new library, start by creating `.def` file.
Structurally it's a simple property file, looking like this:


    header = zlib.h
    compilerOpts = -std=c99

Then run `cinterop` tool with something like (note that for host libraries not included
in sysroot search paths for headers may be needed):

    cinterop -def zlib.def -copt -I/opt/local/include -o zlib.kt.bc

This command will produce `zlib.kt.bc` compiled library and
`zlib.kt.bc-build/kotlin` directory containing Kotlin source code for the library.
``
If behavior for certain platform shall be modified, one may use format like
`compilerOpts.osx` or `compilerOpts.linux` to provide platform-specific values
to options.

Note, that generated bindings are generally platform-specific, so if developing for
multiple targets, bindings need to be regenerated.

After generation of bindings they could be used by IDE as proxy view of the
native library.

For typical Unix library with config script `compilerOpts` will likely contain
output of config script with `--cflags` flag (maybe without exact paths).

Output of config script with `--libs` shall be passed as `-linkedArgs`  `kotlinc`
flag value (quoted) when compiling.

### Selecting library headers

When library headers are imported to C program with `#include` directive,
all of the headers included by these headers are also included to the program.
Thus all header dependencies are included in generated stubs as well.

This behaviour is correct but may be very inconvenient for some libraries. So
it is possible to specify in `.def` file which of the included headers are to
be imported. The separate declarations from other headers may also be imported
in case of direct dependencies.

#### Filtering headers by globs

It is possible to filter header by globs. The `headerFilter` property value
from the `.def` file is treated as space-separated list of globs. If the
included header matches any of the globs, then declarations from this header
are included into the bindings.

The globs are applied to the header paths relative to the appropriate include
path elements, e.g. `time.h` or `curl/curl.h`. So if the library is usually
included with `#include <SomeLbrary/Header.h>`, then it would probably be
correct to filter headers with
```
headerFilter = SomeLbrary/**
```

If `headerFilter` is not specified, then all headers are included.

#### Filtering by module maps

Some libraries have proper `module.modulemap` or `module.map` files among its
headers. For example, macOS and iOS system libraries and frameworks do.
The [module map file](https://clang.llvm.org/docs/Modules.html#module-map-language)
describes the correspondence between header files and modules. When the module
maps are available, the headers from the modules that are not included directly
can be filtered out using experimental `excludeDependentModules` option of the
`.def` file:
```
headers = OpenGL/gl.h OpenGL/glu.h GLUT/glut.h
compilerOpts = -framework OpenGL -framework GLUT
excludeDependentModules = true
```

When both `excludeDependentModules` and `headerFilter` are used, they are
applied as intersection.

### Adding custom declarations ###

Sometimes it is required to add custom C declarations to the library before
generating bindings (e.g. for [macros](#macros)). Instead of creating
additional header file with these declarations, you can include them directly
to the end of the `.def` file, after separating line, containing only the
separator sequence `---`:

```
headers = errno.h

---

static inline int getErrno() {
    return errno;
}
```

Note that this part of the `.def` file is treated as part of the header file, so
functions with body should be declared as `static`.
The declarations are parsed after including the files from `headers` list.

## Using bindings ##

### Basic interop types ###

All supported C types have corresponding representations in Kotlin:

*   Singed, unsigned integral and floating point types are mapped to their
    Kotlin counterpart with the same width.
*   Pointers and arrays are mapped to `CPointer<T>?`.
*   Enums can be mapped to either Kotlin enum or integral values, depending on
    heuristics and definition file hints (see "Definition file hints" below).
*   Structs are mapped to types having fields available via dot notation,
    i.e. `someStructInstance.field1`.
*   `typedef`s are represented as `typealias`es.

Also any C type has the Kotlin type representing the lvalue of this type,
i.e. the value located in memory rather than simple immutable self-contained
value. Think C++ references, as similar concept.
For structs (and `typedef`s to structs) this representation is the main one
and has the same name as the struct itself, for Kotlin enums it is named
`${type}.Var`, for `CPointer<T>` it is `CPointerVar<T>`, and for most other
types it is `${type}Var`.

For those types that have both representations, the "lvalue" one has mutable
`.value` property for accessing value.

#### Pointer types ####

The type argument `T` of `CPointer<T>` must be one of the "lvalue" types
described above, e.g. the C type `struct S*` is mapped to `CPointer<S>`,
`int8_t*` is mapped to `CPointer<int_8tVar>`, and `char**` is mapped to
`CPointer<CPointerVar<ByteVar>>`.

C null pointer is represented as Kotlin's `null`, and the pointer type
`CPointer<T>` is not nullable, but the `CPointer<T>?` is. The values of this
type support all Kotlin operations related to handling `null`, e.g. `?:`, `?.`,
`!!` etc:
```
val path = getenv("PATH")?.toKString() ?: ""
```

Since the arrays are also mapped to `CPointer<T>`, it supports `[]` operator
for accessing values by index:

```
fun shift(ptr: CPointer<BytePtr>, length: Int) {
    for (index in 0 .. length - 2) {
        ptr[index] = ptr[index + 1]
    }
}
```

The `.pointed` property for `CPointer<T>` returns the lvalue of type `T`,
pointed by this pointer. The reverse operation is `.ptr`: it takes the lvalue
and returns the pointer to it.

`void*` is mapped to `COpaquePointer` – the special pointer type which is the
supertype for any other pointer type. So if the C function takes `void*`, then
the Kotlin binding accepts any `CPointer`.

Casting any pointer (including `COpaquePointer`) can be done with
`.reinterpret<T>`, e.g.:
```
val intPtr = bytePtr.reinterpret<IntVar>()
```
or
```
val intPtr: CPointer<IntVar> = bytePtr.reinterpret()
```

As in C, those reinterpret casts are unsafe and could potentially lead to
subtle memory problems in an application.

Also there are unsafe casts between `CPointer<T>?` and `Long` available,
provided by `.toLong()` and `.toCPointer<T>()` extension methods:
```
val longValue = ptr.toLong()
val originalPtr = longValue.toCPointer<T>()
```

Note that if the type of the result is known from the context, the type argument
can be omitted as usual due to type inference.

### Memory allocation ###

The native memory can be allocated using `NativePlacement` interface, e.g.
```
val byteVar = placement.alloc<ByteVar>()
```
or
```
val bytePtr = placement.allocArray<ByteVar>(5):
```

The most "natural" placement is object `nativeHeap`.
It corresponds to allocating native memory with `malloc` and provides additional
`.free()` operation to free allocated memory:

```
val buffer = nativeHeap.allocArray<ByteVar>(size)
<use buffer>
nativeHeap.free(buffer)
```

However the lifetime of allocated memory is often bound to lexical scope.
It is possible to define such scope with `memScoped { ... }`.
Inside the braces the temporary placement is available as implicit receiver,
so it is possible to allocate native memory with `alloc` and `allocArray`,
and the allocated memory will be automatically freed after leaving the scope.

For example, the C function returning values through pointer parameters can be
used like
```
val fileSize = memScoped {
    val statBuf = alloc<statStruct>()
    val error = stat("/", statBuf.ptr)
    statBuf.st_size
}
```

### Passing pointers to bindings ###

Although C pointers are mapped to `CPointer<T>` type, the C function
pointer-typed parameters are mapped to `CValuesRef<T>`. When passing
`CPointer<T>` as the value of such parameter, it is passed to C function as is.
However, the sequence of values can be passed instead of pointer. In this case
the sequence is passed "by value", i.e. the C function receives the pointer to
the temporary copy of that sequence, which is valid only until the function returns.

The `CValuesRef<T>` representation of pointer parameters is designed to support
C array literals without explicit native memory allocation.
To construct the immutable self-contained sequence of C values, the following
methods are provided:

*   `${type}Array.toCValues()`, where `type` is the Kotlin primitive type
*   `Array<CPointer<T>?>.toCValues()`, `List<CPointer<T>?>.toCValues()`
*   `cValuesOf(vararg elements: ${type})`, where `type` is primitive or pointer

For example:

C:
```
void foo(int* elements, int count);
...
int elements[] = {1, 2, 3};
foo(elements, 3);
```

Kotlin:
```
foo(cValuesOf(1, 2, 3), 3)
```

### Working with the strings ###

Unlike other pointers, the parameters of type `const char*` are represented as
Kotlin `String`. So it is possible to pass any Kotlin string to the binding
expecting C string.

There are also available some tools to convert between Kotlin and C strings
manually:

*   `fun CPointer<ByteRef>.toKString(): String`
*   `val String.cstr: CValuesRef<ByteRef>`.

    To get the pointer, `.cstr` should be allocated in native memory, e.g.
    ```
    val cString = kotlinString.cstr.getPointer(nativeHeap)
    ```

In all cases the C string is supposed to be encoded as UTF-8.

### Passing and receiving structs by value ###

When C function takes or returns a struct `T` by value, the corresponding
argument type or return type is represented as `CValue<T>`.

`CValue<T>` is an opaque type, so structure fields cannot be accessed with
appropriate Kotlin properties. It could be acceptable, if API uses structures
as handles, but if field access is required, there are following conversion
methods available:

*   `fun T.readValue(): CValue<T>`. Converts (the lvalue) `T` to `CValue<T>`.
    So to construct the `CValue<T>`, `T` can be allocated, filled and then
    converted to `CValue<T>`.

*   `CValue<T>.useContents(block: T.() -> R): R`. Temporarily places the
    `CValue<T>` to the memory, and then runs the passed lambda with this placed
    value `T` as receiver. So to read a single field, the following code can be
    used:
    ```
    val fieldValue = structValue.useContents { field }
    ```

### Callbacks ###

To convert Kotlin function to pointer to C function,
`staticCFunction(::kotlinFunction)` can be used. It is also allowed to provide
the lambda instead of function reference. The function or lambda must not
capture any values.

Note that some function types are not supported currently. For example,
it is not possible to get pointer to function that receives or returns structs
by value.

#### Passing user data to callbacks ####

Often C APIs allow passing some user data to callbacks. Such data is usually
provided by user when configuring the callback. It is passed to some C function
(or written to the struct) as e.g. `void*`.
However references to Kotlin objects can't be directly passed to C.
So they require wrapping before configuring callback and then unwrapping in
the callback itself, to safely swim from Kotlin to Kotlin through the C world.
Such wrapping is possible with `StableObjPtr` class.

To wrap the reference:
```
val stablePtr = StableObjPtr.create(kotlinReference)
val voidPtr = stablePtr.value
```
where the `voidPtr` is `COpaquePointer` and can be passed to the C function.

To unwrap the reference:

```
val stablePtr = StableObjPtr.fromValue(voidPtr)
val kotlinReference = stablePtr.get()
```
where `kotlinReference` is the original wrapped reference (however it's type is
`Any` so it may require casting).

The created `StableObjPtr` should eventually be manually disposed using
`.dispose()` method to prevent memory leaks:

```
stablePtr.dispose()
```

After that it becomes invalid, so `voidPtr` can't be unwrapped anymore.

See `samples/libcurl` for more details.

### Macros ###

Every C macro that expands to a constant is represented as Kotlin property.
Other macros are not supported. However they can be exposed manually by
wrapping with supported declarations. E.g. function-like macro `FOO` can be
exposed as function `foo` by
[adding the custom declaration](#adding-custom-declarations) to the library:

```
headers = library/base.h

---

static inline int foo(int arg) {
    return FOO(arg);
}
```

### Definition file hints ###

The `.def` file supports several options for adjusting generated bindings.

*   `excludedFunctions` property value specifies a space-separated list of names
    of functions that should be ignored. This may be required because a function
    declared in C header is not generally guaranteed to be really callable, and
    it is often hard or impossible to figure this out automatically. This option
    can also be used to workaround a bug in the interop itself.

*   `strictEnums` and `nonStrictEnums` properties values are space-separated
    lists of the enums that should be generated as Kotlin enum or as integral
    values correspondingly. If the enum is not included into any of these lists,
    than it is generated according to the heuristics.

### Portability ###

Sometimes the C libraries have function parameters or struct fields of
platform-dependent type, e.g. `long` or `size_t`. Kotlin itself doesn't provide
neither implicit integer casts nor C-style integer casts (e.g.
`(size_t) intValue`), so to make writing portable code in such cases easier,
the following methods are provided:

*   `fun ${type1}.signExtend<${type2}>(): ${type2}`
*   `fun ${type1}.narrow<${type2}>(): ${type2}`

where each of `type1` and `type2` must be an integral type.

The `signExtend` converts the integer value to more wide, i.e. the result must
have the same or greater size.
The `narrow` converts the integer value to smaller one (possibly changing the
value due to loosing significant bits), so the result must have the same or
less size.

Any allowed `.signExtend<${type}>` or `.narrow<${type}>` have the same
semantics as one of the `.toByte`, `.toShort`, `.toInt` or `.toLong` methods,
depending on `type`.

The example of using `signExtend`:

```
fun zeroMemory(buffer: COpaquePointer, size: Int) {
    memset(buffer, 0, size.signExtend<size_t>())
}
```

Also the type parameter can be inferred automatically and thus may be omitted
in some cases.
