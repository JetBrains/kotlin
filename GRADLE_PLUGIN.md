# Kotlin/Native Gradle plugin

__Note__: The new __process*InteropName*Interop__ task is used for interop processing instead of the old ones
(__gen*InteropName*InteropStubs__ and __compile*InteropName*InteropStubs__, see [Tasks](#tasks))

## Overview

You may use the Gradle plugin to build _Kotlin/Native_ projects. To use it you need to include the following snippet in
a build script (see projects in `samples` directory):

    buildscript {
       repositories {
           mavenCentral()
           maven {
               url  "https://dl.bintray.com/jetbrains/kotlin-native-dependencies"
           }
       }

       dependencies {
           classpath "org.jetbrains.kotlin:kotlin-native-gradle-plugin:0.3"
       }
    }

    apply plugin: 'konan'

The plugin downloads the compiler during its first run. You may specify a version of the compiler using `konan.version`
project property:

    konan.version=0.3

If you already downloaded the compiler manually you may specify the path to its root directory using `konan.home`
project property (e.g. in `gradle.properties`). Note: the plugin ignores the `konan.version` property in this case.

    konan.home=/home/user/kotlin-native-0.3

In this case the compiler will not be downloaded by the plugin.

To use the plugin you need to define artifacts you want to build in the `konanArtifacts` block. Here you can specify
source files and compilation parameters (e.g. a target platform) for each artifact (see [**Plugin DSL**](#plugin-dsl)
section below for details). The plugin uses `src/main/kotlin/` as a default directory for sources.

    konanArtifacts {
       foo {
           inputFiles fileTree('foo/src')
       }

       bar {
           inputFiles fileTree('bar/src')
           target iphone
       }
    }

## Using C interop

Kotlin/Native provides an easy interaction with native C libraries. The detailed description of this mechanism
can be found in [`INTEROP.md`](INTEROP.md) while this section
describes how to use Gradle to build Kotlin/Native apps using C libraries.

All the C libraies used should be defined in `konanInterop` block.

    konanInterop {
       stdio {
           defFile 'stdio.def'
       }
    }
    
Each element (interop) in this block corresponds to some native library described by a def-file (see
[`INTEROP.md`](INTEROP.md) to read more about def-files). The default path to the def-file of an interop is
`src/main/c_interop/<interop-name>.def`.

Each library is processed by the `cinterop` tool (see [`INTEROP.md`](INTEROP.md) for details). You can specify
additional parameters for this tool in the interop block (e.g. a custom def-file is specified in the example above).
See [**Plugin DSL**](#plugin-dsl) section below for available parameters.

To build an artifact using an interop add `useInterop` command in the artifact section:

    konanArtifacts {
       CsvParser {
           inputFiles project.file('CsvParser.kt')
           useInterop 'stdio'
       }
    }

You also can use an interop from another project:

    evaluationDependsOn 'anotherProject'

    konanArtifacts {
       Foo {
           useInterop project('anotherProject').konanInterop['someInterop']
       }
    }            

## Additional options

You can also pass additional command line keys to the compiler or cinterop tool using the `extraOpts` expression
available an artifact or interop block. For example this sample enables a verbose output for a link and bitcode
generation stages and prints execution time for all compiler phases:

    konanArtifacts
        foo {
            extraOpts '--verbose', 'linker', '--verbose', 'bitcode', '--time'
        }
    }

Any command line key supported by the according tool (compiler or cinterop) can be used. Some of them are listed in the
tables below.

##### Compiler additional options
|Key                |Description                              |
|-------------------|-----------------------------------------|
|`--disable <Phase>`|Disable backend phase                    |
|`--enable <Phase> `|Enable backend phase                     |
|`--list_phases    `|List all backend phases                  |
|`--time           `|Report execution time for compiler phases|
|`--verbose <Phase>`|Trace phase execution                    |
|`-verbose         `|Enable verbose logging output            |

##### Cinterop additional options
|Key                 |Description                                         |
|--------------------|----------------------------------------------------|
|`-verbose <boolean>`|Increase verbosity                                  |
|`-shims <boolean>`  |Add generation of shims tracing native library calls|

## Tasks

The Kotlin/Native plugin creates the following tasks:

* __compileKonan*ArtifactName*__. The plugin creates such a task for each an artifact defined in a `konanArtifacts` block.
You may get this task using the `compilationTask` property of an artifact or by its name:

    ```
    // The task name is "compileKonanFoo"
    konanArtifacts['foo'].compilationTask
    ```
    
    Such a task compiles its artifact and has the following properties accessible from a build script:

    |Property            |Type                        |Description                                       |
    |--------------------|----------------------------|--------------------------------------------------|
    |`outputDir         `|`File`                      |Directory to place the output artifact            |
    |`artifact          `|`File`                      |The output artifact                               |
    |`artifactPath      `|`String`                    |Absolute path to the artifact                     |
    |`produce           `|`String`                    |Kind of the artifact (executable, klib or bitcode)|
    |`inputFiles        `|`Collection<FileCollection>`|Compiled files                                    |
    |`libraries         `|`Collection<FileCollection>`|*.klib libraries used by the artifact             |
    |`nativeLibraries   `|`Collection<FileCollection>`|*.bc libraries used by the artifact               |
    |`interops          `|`Collection<Interop>`       |All the interops used by the artifact             |
    |`linkerOpts        `|`List<String>`              |Additional options passed to the linker           |
    |`enableDebug       `|`boolean`                   |Is the debugging support enabled                  |
    |`noStdLib          `|`boolean`                   |Is the artifact not linked with stdlib            |
    |`noMain            `|`boolean`                   |Is the `main` function provided by a library used |
    |`enableOptimization`|`boolean`                   |Is the optimization enabled                       |
    |`enableAssertions  `|`boolean`                   |Is the assertion support enabled                  |
    |`measureTime       `|`boolean`                   |Does the compiler print phase time                |

* __process*InteropName*Interop__. The plugin creates such a task for each an interop defined in a `konanInterop` block.
You may get this task using `interopProcessingTask` property of an interop object or by its name:

    ```
    // The task name is "processFooInterop"
    konanInterop['foo']. interopProcessingTask
    ```
    
    Such a task processes the library defined by the interop and creates a *.klib for it. The task has the following
    properties accessible from a build script:
    
    |Property       |Type                        |Description                                             |
    |-------------- |----------------------------|--------------------------------------------------------|
    |`outputDir    `|`File`                      |An output directory for the *.klib built                |
    |`klib         `|`File`                      |The *.klib built                                        |
    |`defFile      `|`File`                      |Def-file used by the interop                            |
    |`compilerOpts `|`List<String>`              |Additional options passed to clang                      |
    |`linkerOpts   `|`List<String>`              |Additional options passed to a linker                   |
    |`headers      `|`Collection<FileCollection>`|Additional headers used for stub generation             |
    |`linkFiles    `|`Collection<FileCollection>`|Additional files linked with the stubs                  |
    |`measureTime  `|`boolean`                   |Does the compiler print phase time for stubs compilation|

	_Note_: In versions before 0.3.4 two tasks for each an interop were used: __gen*InteropName*InteropStubs__ and
	__compile*InteropName*InteropStubs__. Now actions of both of them are performed by the
	__process*InteropName*Interop__ task described above.
    
* __compileKonan__. This task is dependent on all compilation tasks and allows one to build all the artifacts supported
by the current host. The task has no properties to use by a build script.

* __run__. This task builds and runs all the executable artifacts supported by the current host. Additional run
parameters can be passed using the `runArgs` project property:

    ```
    ./gradlew run -PrunArgs='foo bar'
    ```
    
    The task has no properties to use by a build script.

The plugin also edits the default `build` and `clean` tasks so that the first one allows one to build all the artifacts
supported (it's dependent on the `compileKonan` task) and the second one removes the files created by the Kotlin/Native
build.

### Task graph

A task dependency structure is simple. Consider a following project:

    konanArtifacts {
        fooArtifact { useInterop 'foo' ... }
        barArtifact { useInterop 'bar' ... }
    }
     
     konanInterop {
        foo { ... }
        bar { ... }
     }
     
For this project the task graph will be the following:

    run
        build
            compileKonan
                compileKonanFooArtifact
                    processFooInterop
                compileKonanBarArtifact
                    processBarInterop
    clean

## Plugin DSL

     konanArtifacts {
         artifactName {
             // Source files
             inputFiles project.fileTree('src')

             // Directory for output artifact (default: build/konan/bin/<artifactName>).
             outputDir 'path/to/output/dir'

             // *.klib library for linking.
             library project.file('path/to/library')
             
             // library project
             library project(':lib')
             
             // artifect in a library project
             library(project(':lib'), 'artefactName')

             // naitve library for linking.
             nativeLibrary project.file('path/to/native/library/')

             // Produce either a 'program' or a 'library' or a bare 'bitcode'.
             produce 'library'     

             noStdLib()            // Don't link with stdlib.
             enableOptimization()  // Enable compiler optimizations.
             enableAssertions()    // Enable assertions in binaries generated.
             enableDebug()         // Enable debugging for binaries generated.

             // Arguments to be passed to a linker.
             linkerOpts 'Some linker opts'

             // Target platform. Available values: "macbook", "linux", "iphone", "raspberrypi".
             target 'macbook'

             // Language and API version.
             languageVersion 'version'
             apiVersion 'version'

             // Native interop to use in the artifact.
             useInterop "interopName"
             
             // Print all parameters during the build.
             dumpParameters true
             
             // Print time of compilation phases (equivalent of the `--time` command line option).
             measureTime true
             
             // Add the `anotherTask` to the compilation task dependencies.
             dependsOn anotherTask
             
             // Pass additional command line options to the compiler.
             extraOpts '--time', '--verbose', 'linker'
        }
     }

     konanInterop {
         interopName {
             defFile project.file("deffile.def")                    // Def-file for stub generation.
             pkg 'org.sample'                                       // Package to place stubs generated.
             target 'macbook'                                       // Target platform.

             // Options to be passed to compiler and linker by cinterop tool.
             compilerOpts 'Options for native stubs compilation'
             linkerOpts 'Options for native stubs'

             headers project.files('header1.h', 'header2.h')       // Additional headers to parse.
             includeDirs "include/directory" "another/directory"   // Directories to look for headers.

             link <files which will be linked with native stubs>   // Additional files to link with native stubs.
             
             dumpParameters true                                   // Print all parameters during the build. 
                                               
             // Add the `anotherTask` to the stub generation task dependencies.
             dependsOn anotherTask
             
             // Add dependency on 'library' in the output klib (analogue of 'depends' parameter in a def-file)
             // where 'library' is a konan interop or konan interop name.
             klibDependsOn 'konanInteropName'
             klibDependsOn konanInterop['foo']
             
             // Pass additional command line options to the cinterop tool.
             extraOpts '-shims', 'true'
         }
     }
