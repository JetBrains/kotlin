# Kotlin/Native backend, Beta version #

## Introduction ##

 _Kotlin/Native_ is an LLVM backend for the Kotlin compiler.
It consists of a machine code generation facility using the LLVM toolchain
and a native runtime implementation.

 _Kotlin/Native_ is primarily designed to allow compilation for platforms where
virtual machines are not desirable or possible (such as iOS or embedded targets),
or where the developer needs to produce a reasonably-sized self-contained binary
that doesn't require an additional execution runtime.

## Supported platforms ##

The _Kotlin/Native_ compiler produces mostly portable (modulo pointer size and target
triplet) LLVM bitcode, and as such can easily support any platform, as long as there's an LLVM
code generator for the platform.
 However, as actualy producing native code requires a platform linker and some
basic runtime shipped along with the translator, we only support a subset of all possible
target platforms. Currently _Kotlin/Native_ is being shipped and tested with support for
the following platforms:

 * Mac OS X 10.11 and later (x86-64), host and target (`-target macos_x64`, default on macOS hosts)
 * Ubuntu Linux x86-64 (14.04, 16.04 and later), other Linux flavours may work as well, host and target
   (`-target linux_x64`, default on Linux hosts, hosted on Linux, Windows and macOS).
 * Microsoft Windows x86-64 (tested on Windows 7 and Windows 10), host and target (`-target mingw_x64`,
   default on Windows hosts). Experimental support is available on Linux and macOS hosts (requires Wine).
 * Microsoft Windows x86-32 cross-compiled target (`-target mingw_x86`), hosted on Windows.
   Experimental support is available on Linux and macOS hosts (requires Wine).
 * Apple iOS (armv7 and arm64 devices, x86 simulator), cross-compiled target
   (`-target ios_arm32|ios_arm64|ios_x64`), hosted on macOS.
 * Linux arm32 hardfp, Raspberry Pi, cross-compiled target (`-target raspberrypi`), hosted on Linux, Windows and macOS
 * Linux MIPS big endian, cross-compiled target (`-target mips`), hosted on Linux.
 * Linux MIPS little endian, cross-compiled target (`-target mipsel`), hosted on Linux.
 * Android arm32 and arm64 (`-target android_arm32|android_arm64`) target, hosted on Linux, macOS and Windows
   (only `android_arm32` at the moment).
 * WebAssembly (`-target wasm32`) target, hosted on Linux, Windows or macOS.
 * Experimental support for Zephyr RTOS (`-target zephyr_stm32f4_disco`) is available on macOS, Linux
   and Windows hosts.

 To enable experimental targets Kotlin/Native must be recompiled with `org.jetbrains.kotlin.native.experimentalTargets` Gradle property set.

 Adding support for other target platforms shouldn't be too hard, if LLVM support is available.

 ## Compatibility and features ##

To run _Kotlin/Native_ compiler JDK 8 or Java 9 or Java 10 (JDK) for the host platform has to be installed.
Produced programs are fully self-sufficient and do not need JVM or other runtime.

On macOS it also requires Xcode 9.4.1 or newer to be installed.

The language and library version supported by this EAP release match Kotlin 1.3.
However, there are certain limitations, see section [Known Limitations](#limitations).

 Currently _Kotlin/Native_ uses reference counting based memory management scheme with a cycle
collection algorithm. Multiple threads could be used, but objects must be explicitly transferred
between threads, and same object couldn't be accessed by two threads concurrently.

_Kotlin/Native_ provides efficient interoperability with libraries written in C or Objective-C, and supports
automatic generation of Kotlin bindings from a C/Objective-C header file.
See the samples coming with the distribution.

  ## Getting Started ##

 Download _Kotlin/Native_ distribution and unpack it. You can run command line compiler with

    bin/kotlinc <some_file>.kt <dir_with_kt_files> -o <program_name>

  During the first run it will download all the external dependencies, such as LLVM.

To see the list of available flags, run `kotlinc -h`.

For documentation on C interoperability stubs see [INTEROP.md](https://github.com/JetBrains/kotlin-native/blob/master/INTEROP.md).

 ## <a name="limitations"></a>Known limitations ##

 ### Performance ###

 *** DO NOT USE THIS PREVIEW RELEASE FOR ANY PERFORMANCE ANALYSIS ***

 This beta version of _Kotlin/Native_ technology is not yet tuned
for benchmarking and competitive analysis of any kind.

### Standard Library ###

  The standard library in _Kotlin/Native_ is known match common standard library in other Kotlin variants.
 Note, that standard Java APIs, such as `java.math.BigDecimal` or `java.io`
is not available in current _Kotlin_ standard library, but using C interoperability, one could
call similar APIs from the POSIX library, see this [`sample`](https://github.com/JetBrains/kotlin-native/blob/master/samples/csvparser).
  Also Kotlin/Native standard library contains certain native-specific extensions, mostly around
memory management and concurrency.

### Reflection ###

Full reflection is not implemented, but class can be referenced and its name could be retrieved.
Notice that property delegation (including lazy properties) *does* work.

### Microsoft Windows support ###

 Only 64-bit Windows is currently supported as compilation host, both 32-bit and 64-bit Windows could
be targets.

### Debugging ###

 _Kotlin/Native_ supports preliminary source-level debugging on produced executables with `lldb` debugger.
 Produce your binary with debugging information by specifying `-g` _Kotlin/Native_ compiler switch.
 Konan plugin accepts `enableDebug` project's property, allowing two ways of producing binaries with the debug
 information:
   - Gradle DSL
   - argument `-PenableDebug=true` in Gradle command line

 Start your application with
    
    lldb my_program.kexe
 
 and then 
    
    command script import tools/konan_lldb.py
    b kfun:main()

to set breakpoint in main function of your application. Single stepping and step into shall work, 
variable inspection may have issues.
See [`DEBUGGING.md`](https://github.com/JetBrains/kotlin-native/blob/master/DEBUGGING.md).
