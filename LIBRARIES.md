 # Kotlin/Native libraries

  ## Kotlin compiler specifics

To produce a library with Kotlin/Native compiler use `-produce library` or `-p library` flag. For example:

    $ kotlinc foo.kt -p library -o bar

the above command will produce a `bar.klib` with compiled contents of `foo.kt`.

To link a library use `-library <name>` or `-l <name>` flag. For example:

    $ kotlinc qux.kt -l bar

the above command will produce `program.kexe` out of `qux.kt` and `bar.klib`


  ## cinterop tool specifics

The **cinterop** tool produces `.klib` wrappers for native libraries as its main output. 
For example using the simple `stdio.def` native library definition file provided in your Kotlin/Native distribution

    $ cinterop -def  ./samples/csvparser/src/main/c_interop/stdio.def  -o stdio

we obtain `stdio.klib`. 


  ## klib utility

The **klib** library management utility allows one to inspect and install the libraries.

The following commands are available.

To list library contents:

        $ klib contents <name>

To inspect the bookkeeping details of the library 

        $ klib info <name>

To install the library to the default location use

        $ klib install <name>

To remove the library from the default repository use 

        $ klib remove <name>

All of the above commands accept an additional `-repository <directory>` argument to specify a repository other than the default one. 

        $ klib <command> <name> -repository <directory>


  ## Several examples

First lets create a library:

    $ cinterop -h /usr/include/math.h -pkg libc.math -o math

The library has been created in the current directory:

    $ ls math.klib
    math.klib

Now let's check out the contents of the library:

    $ klib contents math

We can install `math` to the default repository:

    $ klib install math

Remove any traces of it and its build process from the current directory:

    $ rm -rf ./math*

Create a very short program and place it into a `sin.kt` :

    import libc.math.*
    fun main(args: Array<String>) {
        println(sin(2.0))
    }

Now compile the program linking with the library we have just created:

    $ kotlinc sin.kt -l math -o mysin

And run your program:

    $ ./mysin.kexe
    0.9092974268256817

Have fun!

  # Advanced topics

 ## Library search sequence

When given `-library foo` flag, the compiler searches the `foo`  library in the following order:

    * Current compilation directory or an absolute path.

    * All repositories specified with `-repo` flag.

    * Libraries installed in the default repository (For now the default is  `~/.konan`, however it could be changed by setting **KONAN_DATA_DIR** environment variable).

    * Libraries installed in `$installation/klib` directory.


 ## The library format

**WARNING**: the library format is *very* preliminary. It is subject to change right under your fingers. And it can incompatibly change from release to release until Kotlin/Native is stabilized.

Kotlin/Native libraries are zip files containing predefined 
directory structure, with the following layout:

**foo.klib** when unpacked as **foo/** gives us:

  - foo/
    - targets/
      - $platform/
        - kotlin/
          - Kotlin compiled to LLVM bitcode.
        - native/
          - Bitcode files of additional native objects.
      - $another_platform/
        - There can be several platform specific kotlin and native pairs.
    - linkdata/
      - A set of ProtoBuf files with serialized linkage metadata.
    - resources/
      - General resources such as images. (Not used yet).
    - manifest - A file in *java property* format describing the library.

    An exemplar layout can be found in `klib/stdlib` directory of your installation.

