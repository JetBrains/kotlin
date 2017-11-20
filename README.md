[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

# Kotlin/Native  #

_Kotlin/Native_ is a LLVM backend for the Kotlin compiler, runtime
implementation and native code generation facility using LLVM toolchain.

 _Kotlin/Native_ is primarily designed to allow compilation for platforms where
virtual machines are not desirable or possible (such as iOS, embedded targets),
or where a developer is willing to produce a reasonably-sized self-contained program
without the need to ship an additional execution runtime.

Prerequesties:
	install JDK for your platform, instead of JRE. The build requires ```tools.jar```, which is not included in JRE.

To compile from sources use following steps.

First download dependencies:

	./gradlew dependencies:update

Then build the compiler and libraries:

	./gradlew bundle

The build can take about an hour on a Macbook Pro.
To run a shorter build with only host compiler and libraries run:

    ./gradlew dist distPlatformLibs

After that you should be able to compile your programs like that:

    export PATH=./dist/bin:$PATH
	kotlinc hello.kt -o hello

For an optimized compilation use `-opt`:

	kotlinc hello.kt -o hello -opt

For some tests, use:

	./gradlew backend.native:tests:run

To generate interoperability stubs create library definition file
(take a look on [`samples/tetris/.../sdl.def`](https://github.com/JetBrains/kotlin-native/blob/master/samples/tetris/src/main/c_interop/sdl.def)) and run `cinterop` tool like this:

    cinterop -def lib.def

See provided [samples](https://github.com/JetBrains/kotlin-native/tree/master/samples) and [`INTEROP.md`](https://github.com/JetBrains/kotlin-native/blob/master/INTEROP.md) for more details.

Interop tool generates library in .klib library format, see [`LIBRARIES.md`](https://github.com/JetBrains/kotlin-native/blob/master/LIBRARIES.md)
for more details on the file format.
