# Early Access Preview of Kotlin Native #

## Introduction ##

 Kotlin Native backend, codenamed _Kotlin N_, is a LLVM backend for the Kotlin
compiler. It consists of a native code generation facility using the LLVM toolchain
and a native runtime implementation.

 _Kotlin N_ is primarily designed to allow compilation for platforms where
virtual machines are not desirable or possible (such as iOS, embedded targets),
or where the developer needs to produce a reasonably-sized self-contained program
that doesn't require an additional execution runtime.

## Supported platforms ##

 _Kotlin N_ compiler produces mostly portable (modulo pointer size and target
triplet) LLVM bitcode, and as such can easily support any platform, as long as there's a LLVM
codegenerator for the platform.
 However, as actual producing of the native code requires a platform linker and some
basic runtime shipped with the translator, we only support a subset of all possible
target platforms. Currently _Kotlin N_ is being shipped and tested with support for
the following platforms:

 * Mac OS X 10.10 and later (x86-64)
 * x86-64 Ubuntu Linux (14.04, 16.04 and later), other Linux flavours may work as well
 * Apple iOS (arm64), cross-compiled on MacOS X host (`-target iphone`)
 * Raspberry Pi, cross-compiled on Linux host (`-target raspberrypi`)


 Adding support for other target platforms shouldn't be too hard, if LLVM support
 is available.

 ## Compatibility and features ##

The language and library version supported by this EAP release mostly match Kotlin 1.1.
However, there are certain limitations, see section [Known Limitations](#limitations).

 Currently _Kotlin N_ uses reference counting based memory management scheme with cycles
garbage collection algorithm. Multiple threads could be used, but no objects shared 
between threads are allowed.

_Kotlin N_ provides efficient interoperability with libraries written in C, and supports
automatic generation of Kotlin bindings from a C header file.
See samples coming with the distribution.

  ## Getting Started ##

 Download Kotlin N distribution and unpack it. You can run command line compiler with

	bin/kotlinc <some_file>.kt <dir_with_kt_files> -o <executable>.kexe

  During the first run it will download all the external dependencies, such as LLVM.

To see the list of available flags, run `kotlinc -h`.

For documentation on C interoperability stubs see INTEROP.md.

 ## <a name="limitations"></a>Known limitations ##

 ### Performance ###

 *** DO NOT USE THIS PREVIEW RELEASE FOR ANY PERFORMANCE ANALYSIS ***

 This is purely a technology preview of Kotlin N technology, and is not yet tuned
for benchmarking and competitive analysis of any kind.

### Standard Library ###

  The standard library in Kotlin N is known to be incomplete and doesn't include
certain methods available in standard library of Kotlin.

### Coroutines ###

Coroutines are not yet supported with this release.

### Reflection ###

Full reflection and class object references are not implemented.
Notice that property delegation (including lazy properties) *does* work.

### Microsoft Windows support ###

   Due to significant difference in exception handling model on MS Windows and
other LLVM targets, current _Kotlin N_ may not produce executables working on
MS Windows. This situation could be improved in upcoming releases.
