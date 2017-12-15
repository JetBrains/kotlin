## v0.5 (Dec 2017)
  * Reverse interop allowing to call Kotlin/Native code compiled as framework from Objective-C/Swift programs
  * Reverse interop allowing to call Kotlin/Native code compiled as shared object from C/C++ programs
  * Support generation of shared objects and DLLs by the compiler
  * Migration to LLVM 5.0
  * Support WebAssembly target on Linux and Windows hosts
  * Make string conversions more robust
  * Support kotlin.math package
  * Refine workers and string conversion APIs

## v0.4 (Nov 2017) ##
  * Objective-C frameworks interop for iOS and macOS targets
  * Platform API libraries for Linux, iOS, macOS and Windows
  * Kotlin 1.2 supported
  * `val` and function parameters can be inspected in debugger
  * Experimental support for WebAssembly (wasm32 target)
  * Linux MIPS support (little and big endian, mips and mipsel targets)
  * Gradle plugin DSL fully reworked
  * Support for unit testing annotations and automatic test runner generation
  * Final executable size reduced
  * Various interop improvements (forward declaration, better handling of unsupported types)
  * Workers object subgraph transfer checks implemented
  * Optimized low level memory management using more efficient cycle tracing algorithm

## v0.3.4 (Oct 2017) ##
  * Intermediate release

## v0.3.2 (Sep 2017) ##
  * Bug fixes

## v0.3.1 (Aug 2017) ##
  * Improvements in C interop tools (function pointers, bitfields, bugfixes)
  * Improvements to Gradle plugin and dependency downloader
  * Support for immutable data linked into an executable via ImmutableDataBlob class
  * Kotlin 1.1.4 supported
  * Basic variable inspection support in the debugger
  * Some performance improvements ("for" loops, memory management)
  * .klib improvements (keep options from .def file, faster inline handling)
  * experimental workers API added (see [`sample`](https://github.com/JetBrains/kotlin-native/blob/master/samples/workers))

## v0.3 (Jun 2017) ##
  * Preliminary support for x86-64 Windows hosts and targets
  * Support for producing native activities on 32- and 64-bit Android targets
  * Extended standard library (bitsets, character classification, regular expression)
  * Preliminary support for Kotlin/Native library format (.klib)
  * Preliminary source-level debugging support (stepping only, no variable inspection)
  * Compiler switch `-entry` to select entry point
  * Symbolic backtrace in runtime for unstripped binaries, for all supported targets

## v0.2 (May 2017) ##
  * Added support for coroutines
  * Fixed most stdlib incompatibilities
  * Improved memory management performance
  * Cross-module inline function support
  * Unicode support independent from installed system locales
  * Interoperability improvements
     * file-based filtering in definition file
     * stateless lambdas could be used as C callbacks
     * any Unicode string could be passed to C function
  * Very basic debugging support
  * Improve compilation and linking performance

## v0.1 (Mar 2017) ##
Initial technical preview of Kotlin/Native