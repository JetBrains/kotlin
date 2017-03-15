# Kotlin N interoperability #

## Introduction ##

 _Kotlin N_ follows general tradition of Kotlin to provide excellent
existing platform software interoperability. In case of native platform
most important interoperability target is a C library. Thus _Kotlin N_
comes with an `interop` tool, which could be used to quickly generate
everything needed to interact with an external library.

 Following workflow is expected when interacting with the native library.
   * create `.def` file describing what to include into bindings
   * use `interop` tool to produce `stubs.bc` and Kotlin bindings
   * run _Kotlin N_ compiler on an application to produce the final executable

 Interoperability tool analyses C headers and produces "natural" mapping of
types, function and constants into the Kotlin world. Generated stubs can be
imported into an IDE for purposes of code completion and navigation.

## Simple example ##

Build the dependencies and the compiler (see README.md).

Prepare stubs for the system sockets library:

    ./dist/bin/interop -def:backend.native/tests/interop/basics/sockets.def

Compile the echo server:

    ./dist/bin/kotlinc backend.native/tests/interop/basics/echo_server.kt \
        sockets -nativelibrary socketsstubs.bc 

Run the server:

    ./program.kexe 3000 &

Test the server by conecting to it, for example with telnet:

    telnet localhost 3000

Write something to console and watch server echoing it back.

~~Quit telnet by pressing ctrl+] ctrl+D~~


## Creating bindings for a new library ##

 To create bindings for a new library, start by creating `.def` file.
Structurally it's a simple property file, looking like this:


    header = zlib.h
    compilerOpts = -std=c99
    linkerOpts = -lz

Then run interop tool with something like (note that for host libraries not included
in sysroot search paths for headers may be needed):

    ./dist/bin/interop -def:zlib.def -copt:-I/opt/local/include

This command will produce directory named `zlib` containing file `zlib.kt`
and file `zlibstubs.bc` containing implementation specific glue bitcode.

If behavior for certain platform shall be modified, one may use format like
`compilerOpts.osx` or `compilerOpts.linux` to provide platform-specific values
to options.

Note, that generated bindings are generally platform-specific, so if developing for
multiple targets, bindings need to be regenerated.

After generation of bindings they could be used by IDE as proxy view of the
native library.

For typical Unix library with config script `compilerOpts` will likely contain
output of config script with `--cflags` flag (maybe without exact paths) and
`linkerOpts` - output of config script with `--libs`.

Also all those values could be passed directly as values for `-copt` and
`linkedArgs` respectively.
