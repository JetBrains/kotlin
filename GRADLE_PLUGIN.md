# Kotlin/Native Gradle plugin

#### Overview
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

If you already downloaded the compiler manually you may specify the path to it using `konan.home` project property (e.g.
 in `gradle.properties`). Note: the plugin ignores the `konan.version` property in this case.

    konan.home=/path/to/already/downloaded/compiler

In this case the compiler will not be downloaded by the plugin.

To use the plugin you need to define artifacts you want to build in the `konanArtifacts` block. Here you can specify
source files and compilation parameters (e.g. a target platform) for each artifact (see **Plugin DSL** section below for
details). The plugin uses `src/main/kotlin/` as a default directory for sources.

    konanArtifacts {
       foo {
           inputFiles fileTree('foo/src')
       }

       bar {
           inputFiles fileTree('bar/src')
           target iphone
       }
    }

If you want to interact with native C libraries you need to define them in `konanInterop` block and add the defined
interop in the artifact definition using `useInterop` method.

    konanInterop {
       stdio {
           defFile 'stdio.def'
       }
    }

    konanArtifacts {
       CsvParser {
           inputFiles project.file('CsvParser.kt')
           useInterop 'stdio'
       }
    }

Each element in the `konanInterop` block creates a task for `cinterop` tool exection (see `INTEROP.md` to read more
about this tool) so you can specify `cinterop` parameters here (see **Plugin DSL** section below). The default path to
an interop def-file is `src/main/c_interop/<interop-name>.def`.

One can get a task for Kotlin/Native compilation using the `compilationTask` artifact property or by name:
`compileKonan<ArtifactName>`.

    konanArtifacts['foo'].compilationTask    // The task name is "compileKonanFoo"

One can get `cinterop` execution and stub compilation tasks using `generateStubsTask` and `compileStubsTask`
interop properties or by names: `gen<InteropName>InteropStubs` (`cinterop` execution task) and
`compile<InteropName>InteropStubs` (stub compilation task).

    konanInterop['stdio'].generateStubsTask  // The task name is "genStdioInteropStubs"
    konanInterop['stdio'].compileStubsTask   // The task name is "compileStdioInteropStubs"

All the tasks contain a set of properties allowing one to obtain paths to artifacts built and parameters of compilation/cinterop
execution. One may print all these parameters by specifying `dumpParamters(true)` in an interop or an artifact block:
    
    konanArtifacts {
       foo { dumpParameters(true) }
    }
    
The plugin creates two additional tasks: `compileKonan` and `run`. The first one is dependent on all compilation
tasks and allows one to build all the artifacts supported by the current host. The second one allows one to run all
executable artifacts supported by the current host. Additional parameters can be passed using the `runArgs` project
property:

    ./gradlew run -PrunArgs='foo bar'

The plugin also edits the default `build` and `clean` tasks so that the first one allows one to build all the artifacts
supported (it's dependent on the `compileKonan` task) and the second one removes the files created by the Kotlin/Native
build.

#### Task graph

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
                    compileFooInteropStubs
                        genFooInteropStubs
                compileKonanBarArtifact
                    compileBarInteropStubs
                        genBarInteropStubs
    clean

#### Plugin DSL

     konanArtifacts {
         artifactName {
             // Source files
             inputFiles project.fileTree('src')

             // Directory for output artifact (default: build/konan/bin/<artifactName>).
             outputDir 'path/to/output/dir'

             // *.klib library for linking.
             library project.file('path/to/library')

             // naitve library for linking.
             nativeLibrary project.file('path/to/native/library/')

             // Produce either a 'program' or a 'library' or a bare 'bitcode'.
             produce 'library'     

             noStdLib()            // Don't link with stdlib.
             enableOptimization()  // Enable compiler optimizations.
             enableAssertions()    // Enable assertions in binaries generated.
             enableDebug()         // Enable debugging for binaries generated.

             manifest 'path/to/manifest.file' // A manifest addend file.

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
             dumpParameters(true)
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

             link <files which will be linked with native stubs>    // Additional files to link with native stubs.
             
             dumpParameters(true)                                   // Print all parameters during the build.                       
         }
     }
