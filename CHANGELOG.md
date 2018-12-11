## v1.1.0 (Dec 2018)
  * Performance optimizations:
    * runtime: optimization of queue of finalization
    * compiler: loop generation optimization
	* compiler: reduce RTTI size
	* runtime: reduce size of the object header
  * Contracts support
  * Regex engine: fix quantifier processing

## v0.9.3 (Sep 2018)
  * Bugfixes

## v0.9.2 (Sep 2018)
  * Support Xcode 10.0
  * iOS 9.0 is the minimal supported version for all targets
  * Swift interop improvements
  * Support shared top level values of some immutable types (i.e. String and atomic references)
  * Support release Kotlin 1.3.0

## v0.9.1 (Sep 2018)
  * Improve naming in produced Objective-C frameworks. Use ‘Kotlin’ prefix instead of ‘Stdlib’ prefix.
  * Improvements in KLIB: Library versioning, IDEA-friendly internal format.

# v0.9 (Sep 2018)
  * Support Kotlin 1.3M2
    * Note: Common modules of multiplatform projects also should use Kotlin 1.3
  * Major standard library (native parts) rework and rename
  * New Gradle plugin with multiplatform integration and reworked DSL
  * Support unsigned types in Kotlin and interop
  * Support non-experimental coroutines API (kotlin.coroutines)
  * Top level object var/val can only be accessed from the main thread
  * Support lazy properties in singleton objects
  * Update LLVM to 6.0.1

## v0.8 (Jul 2018)
  * Singleton objects are frozen after creation, and shared between threads
  * String and primitives types are frozen by default
  * Common stdlib with Kotlin/JVM and Kotlin/JS
  * Implemented `kotlin.random.*` and `Collection.shuffle`
  * Implemented atomic integers and atomic references
  * Multiple bugfixes in compiler (coroutines, inliner)
  * Support 32-bit iOS (target `ios_arm32`)
  * New experimental Gradle plugin
  * Support Xcode 9.4.1
  * Optimizations (switch by enum, memory management)

## v0.7.1 (Jun 2018)
  * Bugfixes in the runtime (indexOf, GC for kotlin.Array, enum equality) and the compiler
  * Fix NSBlock problem, preventing upload of binaries to the AppStore
  * Create primitive type boxes and kotlin.String as frozen by default
  * Support Gradle 4.7, provide separate run task for each executable
  * Support Xcode 9.4 and CoreML and ClassKit frameworks on Apple platforms
  * Improved runtime Kotlin variable examination
  * Minor performance optimizations in compiled code and runtime
  * Add `disableDesignatedInitializerChecks` definition file support

## v0.7 (May 2018)
  * Interop with Objective-C/Swift changes:
     * Uniform direct and reverse interops (values could be passed in both directions now)
     * Interop by exceptions
     * Type conversion and checks (`as`, `is`) for interop types
     * Seamless interop on numbers, strings, lists, maps and sets
     * Better interop on constructors and initializers
  * Switched to Xcode 9.3 on Apple platforms
  * Introduced object freezing API, frozen object could be used from multiple threads
  * Kotlin enums are frozen by default
  * Switch to Gradle 4.6
  * Use Gradle native dependency model, allowing to use `.klib` as Maven artifacts
  * Introduced typed arrays API
  * Introduced weak references API
  * Activated global devirtualization analysis
  * Performance improvements (box caching, bridge inlining, others)

## v0.6.2 (Mar 2018)
  * Support several `expectedBy`-dependencies in Gradle plugin.
  * Improved interaction between Gradle plugin and IDE.
  * Various bugfixes

## v0.6.1 (Mar 2018)
  * Various bugfixes
  * Support total ordering in FP comparisons
  * Interop generates string constants from string macrodefinitions
  * STM32 blinky demo in pure Kotlin/Native
  * Top level variables initialization redesign (proper dependency order)
  * Support kotlin.math on WebAssembly targets
  * Support embedded targets on Windows hosts

## v0.6 (Feb 2018)
  * Support multiplatform projects (expect/actual) in compiler and Gradle plugin
  * Support first embedded target (STM32 board)
  * Support Kotlin 1.2.20
  * Support Java 9
  * Support Gradle 4.5
  * Transparent Objective-C/Kotlin container classes interoperability
  * Produce optimized WebAssembly binaries (10x smaller than it used to be)
  * Improved APIs for object transfer between threads and workers
  * Allow exporting top level C function in reverse interop with @CName annotation
  * Supported debugging of code with inline functions
  * Multiple bugfixes and performance optimizations

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
