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
           classpath "org.jetbrains.kotlin:kotlin-native-gradle-plugin:0.2"
       }
    }

    apply plugin: 'konan'

The plugin downloads the compiler during its first run. You may specify a version of the compiler using `konan.version`
project property:

    konan.version=0.2

If you already downloaded the compiler manually you may specify the path to it using `konan.home` project property (e.g.
 in `gradle.properties`). Note: the plugin ignores the `konan.version` property in this case.

    konan.home=/path/to/already/downloaded/compiler

In this case the compiler will not be downloaded by the plugin.

To use the plugin you need to define artifacts you want to build in the `konanArtifacts` block. Here you can specify
source files and compilation parameters (e.g. a target platform) for each artifact (see **Plugin DSL** section below for
details).

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
about this tool) so you can specify `cinterop` parameters here (see **Plugin DSL** section below).

You can get a task for Kotlin/Native compilation using `compilationTask` property of an artifact:

      konanArtifacts['foo'].compilationTask

You can get a task for `cinterop` execution and a stub compilation task using `generateStubsTask` and `compileStubsTask`
of an interop defined:

    konanInterop['stdio'].generateStubsTask
    konanInterop['stdio'].compileStubsTask

All tasks contain a set of properties allowing one to obtain paths to artifacts built and paramters of compilation and
cinterop execution (see the `dumpParameters` task in `samles/csvparser/build.gradle` for example).

#### Plugin DSL

     konanArtifacts {
         artifactName {
             // Source files
             inputFiles project.fileTree('src')

             // Directory for output artifact (default: build/konan/bin/<artifactName>).
             outputDir 'path/to/output/dir'

             // *.kt.bc library for linking.
             library project.file('path/to/library')

             // naitve library for linking.
             nativeLibrary project.file('path/to/native/library/')

             noStdLib()            // Don't link with stdlib.
             noLink()              // Don't link, just produce a bitcode file.
             enableOptimization()  // Enable compiler optimizations.

             // Arguments to be passed to a linker.
             linkerOpts 'Some linker opts'

             // Target platform. Available values: "macbook", "linux", "iphone", "raspberrypi".
             target 'macbook'

             // Language and API version.
             languageVersion 'version'
             apiVersion 'version'

             // Native interop to use in the artifact.
             useInerop "interopName"
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
         }
     }