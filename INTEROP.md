# An example of interop tool use #

Build the dependencies and the compiler (see README.md).

Prepare stubs for the system sockets library:

    ./dist/bin/interop -def:backend.native/tests/interop/basics/sockets.def

Compile the echo server:

    ./dist/bin/konanc backend.native/tests/interop/basics/echo_server.kt \
        sockets -nativelibrary socketsstubs.bc 

Run the server:

    ./program.kexe 3000 &

Test the server by conecting to it, for example with telnet:

    telnet localhost 3000

Write something to console and watch server echoing it back.

~~Quit telnet by pressing ctrl+] ctrl+D~~

