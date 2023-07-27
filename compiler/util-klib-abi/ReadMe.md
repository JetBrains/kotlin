## API for extracting publicly visible ABI from KLIBs

This is the API to extract and dump declarations from KLIBs that comprise publicly visible part of KLIB ABI. Can be used for implementing various KLIB-oriented build tools that do ABI compatibility validation, perform compilation avoidance with KLIBs, etc.

There are two major entry points:
* [LibraryAbiReader](src/org/jetbrains/kotlin/library/abi/LibraryAbiReader.kt) - extracts publicly visible ABI
* [LibraryAbiRenderer](src/org/jetbrains/kotlin/library/abi/LibraryAbiRenderer.kt) - renders it to a human-readable textual representation
