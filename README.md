# Kotlin/Native  #

_Kotlin/Native_ is a LLVM backend for the Kotlin compiler, runtime
implementation and native code generation facility using LLVM toolchain.

 _Kotlin/Native_ is primarily designed to allow compilation for platforms where
virtual machines are not desirable or possible (such as iOS, embedded targets),
or where developer is willing to produce reasonably-sized self-contained program
without need to ship an additional execution runtime.

To compile from sources use following steps.

First download dependencies:

	./gradlew dependencies:update

Then build the compiler and standard library:

	./gradlew dist

To build standard library for cross-targets (currently, iOS on Mac OSX and Raspberry Pi on
Linux hosts) use:

    ./gradlew cross_dist

After that you should be able to compile your programs like that:

    export PATH=./dist/bin:$PATH
	kotlinc hello.kt -o hello

For an optimized compilation use `-opt`:

	kotlinc hello.kt -o hello -opt

For some tests, use:

	./gradlew backend.native:tests:run

To generate interoperability stubs create library definition file
(take a look on [`samples/tetris/sdl.def`](https://github.com/JetBrains/kotlin-native/blob/master/samples/tetris/sdl.def)) and run `cinterop` tool like this:

    cinterop -def lib.def

See provided [samples](https://github.com/JetBrains/kotlin-native/tree/master/samples) and [`INTEROP.md`](https://github.com/JetBrains/kotlin-native/blob/master/INTEROP.md) for more details.
