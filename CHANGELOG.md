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