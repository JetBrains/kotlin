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
    ../../dist/bin/cinterop -def:sockets.def -o:sockets.kt.bc

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

    cinterop -def:zlib.def -copt:-I/opt/local/include -o:zlib.kt.bc

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
