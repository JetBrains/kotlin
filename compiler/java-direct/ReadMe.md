
# Java Facade for Kotlin compiler

The module is intended to replace the PSI-based facade that pulls a lot of IntelliJ platform code into compiler
with a lightweight "direct" implementation.

## Status

The module is functional ind integrated into the compiler via the compiler option (`-Xjava-direct`) and 
the language feature (`JAVA_DIRECT`).
The old PSI-based java class finder is still used for binary classes (via `CombinedJavaClassFinder`) due to some 
quirks of the FIR providers architecture. On the next iteration it should be replaced with FIR-based symbol providers.

## Purpose

Kotlin has bidirectional Java interop, meaning that in a module there could be both Java files referencing Kotlin declarations
and vice versa. Therefore, Kotlin compiler cannot rely on Java files being available in a binary form, when the Kotlin sources 
are compiled and need to process Java sources directly, extract the declarations and make them accessible for the FIR resolution.
This was implemented initially via the infrastructure from the IntelliJ platform, often referred to as PSI-based Java facade.
(There were also an attempt to implement it via unofficial `javac` APIs, but it wasn't properly supported.)

## Architecture

### Output

The module provides so-called "Java model" as the output, that is the implementation of the interfaces defined
in the `org.jetbrains.kotlin.load.java.structure` package in the `core.jvm` module.

The access is provided via the implementation of the `JavaClassFinder` innerface.

### Laziness

Since we only need to consider Java declarations, which are accessed from FIR resolution, and there could be modules with many Java files
and very little interop in this direction, the implementation is made as lazy as possible. It starts with source roots analysis:
for the directory-based roots we consider that the directory structure should correspond to the package structure and only access
files when the corresponding package is requested. For the file-based roots the files are scanned without parsing to extract the
package name and top-level classes and then parsed only if later requested from FIR.
Parsing is done eagerly into a light-tree structure (see below), but further extraction of the Java Model is also done lazily with some
caching on top.

### Parser

The module uses lightweight KMP parsers infrastructure being developed in the IntelliJ platform, but without pulling the heavy
parts of the platform with it. The libraries (`org.jetbrains:syntax-api` and `org.jetbrains:java-syntax`) are extracted and published
independently by the Fleet team. 

The parsing produces a light-tree structure similar to the Kotlin light-tree parser, but without any use of the IntelliJ platform specific 
infrastructure.

### Resolution

The Java model requires symbol resolution, for many cases, such as references to Java source declarations in the same module, references 
to library declarations, and references to Kotlin declarations.
In contrast to the PSI-based facade, the `java-direct` module uses FIR-based resolution for all non-Java-sources references, via the newly
added callback mechanism.

### Integration

The new java class finder factory extension is introduced to allow substituting the implementation from, e.g., a compiler plugin.
Due to some technical difficulties, the current implementation is not available as an independent plugin, but it probably should be in 
the future.

### Tests

The module contains unit tests and also "steals" all phased diagnostics and box tests that contain Java files from the main compiler
testdata.
