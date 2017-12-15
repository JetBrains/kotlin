# Kotlin/Native  #

_Kotlin/Native_ is a LLVM backend for the Kotlin compiler, runtime
implementation and native code generation facility using LLVM toolchain.

 _Kotlin/Native_ is primarily designed to allow compilation for platforms where
virtual machines are not desirable or possible (such as iOS, embedded targets),
or where developer is willing to produce reasonably-sized self-contained program
without need to ship an additional execution runtime.

 To get started with _Kotlin/Native_ take a look at the attached samples.

  * `androidNativeActivity` - Android Native Activity rendering 3D graphics using OpenGLES
  * `calculator` - iOS Swift application, using Kotlin/Native code compiled into the framework
  * `csvparser` - simple CSV file parser and analyzer
  * `gitchurn` - program interoperating with `libgit2` for GIT repository analysis
  * `gtk` - GTK2 interoperability example
  * `html5Canvas` - WebAssembly example
  * `libcurl` - using of FTP/HTTP/HTTPS client library `libcurl`
  * `nonBlockingEchoServer` - multi-client TCP/IP echo server using co-routines
  * `objc` - AppKit Objective-C interoperability example for macOS
  * `opengl` - OpenGL/GLUT teapot example
  * `python_extension` - Python extension written in Kotlin/Native
  * `socket` - TCP/IP echo server
  * `tensorflow` - simple client for TensorFlow Machine Intelligence library
  * `tetris` - Tetris game implementation (using SDL2 for rendering)
  * `uikit` - UIKit Objective-C interoperability example for iOS
  * `videoplayer` - SDL and FFMPEG-based video and audio player
  * `win32` - trivial Win32 GUI application
  * `workers` - example of using workers API


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