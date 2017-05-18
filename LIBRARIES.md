 ## Kotlin/Native library format

**WARNING**: the format is *very* preliminary. It is subject to change right under your fingers.

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

    An exemplar layout can be found in **klib** directory of your installation.

 ## Library search sequence

When given **-library foo** flag, the compiler searches the libraries in the following order:

    * Current compilation directory or an absolute path.

    * All repositories specified with *-repo* flag.

    * Libraries installed in .konan directory.

    * Libraries installed in $installation/klib directory.

  ## **cinterop* tool specifics

The **cinterop** tool produces **klib** wrappers for native libraries. 


  ## **klib** utility

The **klib** library management utility allows one to inspect and install the libraries.

The following commands are available.

To ask the details of the library 

        $ klib info foo

To list library contents:

        $ klib list foo

To install the library to the default location use

        $ klib install foo

To install the library to another location run

        $ klib install foo -repository <directory>

To remove the library from the repository use

        $ klib remove foo [-repository <directory>]

