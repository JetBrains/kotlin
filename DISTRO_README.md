# Kotlin/Native  #

_Kotlin/Native_ is a LLVM backend for the Kotlin compiler, runtime
implementation and native code generation facility using LLVM toolchain.

 _Kotlin/Native_ is primarily designed to allow compilation for platforms where
virtual machines are not desirable or possible (such as iOS, embedded targets),
or where developer is willing to produce reasonably-sized self-contained program
without need to ship an additional execution runtime.

 To get started with _Kotlin/Native_ take a look at the attached samples.

  * `csvparser` - simple CSV file parser and analyzer
  * `gitchurn` - program interoperating with `libgit2` for GIT repository analysis
  * `libcurl` - using of FTP/HTTP/HTTPS client library `libcurl`
  * `opengl` - OpenGL/GLUT teapot example
  * `socket` - TCP/IP echo server
  * `tetris` - Tetris game implementation (using SDL2 for rendering)

 See `README.md` in each sample directory for more information and build instructions.

 _Kotlin/Native_ could be used either as standalone compiler toolchain or as Gradle
plugin. See `GRADLE_PLUGIN.md` for more details on how to use this plugin.

Compile your programs like that:

    export PATH=kotlin-native-<platform>-<version>/bin:$PATH
	kotlinc hello.kt -o hello

For an optimized compilation use -opt:

	kotlinc hello.kt -o hello -opt

To generate interoperability stubs create library definition file
(take a look on `samples/tetris/tetris.sdl`) and run `cinterop` tool like this:

    cinterop -def lib.def
 
See `INTEROP.md` for more information on how to use C libraries from _Kotlin/Native_.

See `RELEASE_NOTES.md` for information on supported platforms and current limitations.