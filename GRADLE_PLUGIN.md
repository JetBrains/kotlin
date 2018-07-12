# Kotlin/Native Gradle plugin

_Note: In the version 0.8 a new experimental plugin has been introduced. It is integrated with new Gradle support
for native languages and provides a new DSL which is much closer to the DSL of Kotlin/JVM and Kotlin/JS
plugins than the old one. The plugin is at an early stage so some things may be changed in upcoming releases. See
[this demo project](https://github.com/ilmat192/Kotlin-Native-Gradle-Experiments) or
[MPP http client](https://github.com/e5l/http-client-common/tree/master/samples/ios-test-application) for DSL example._

## Overview

You may use the Gradle plugin to build _Kotlin/Native_ projects. Since version 0.8 release builds of the plugin are
[available](https://plugins.gradle.org/plugin/org.jetbrains.kotlin.konan) at the Gradle plugin portal so you can apply it
using Gradle plugin DSL:

    plugins {
        id "org.jetbrains.kotlin.konan" version "0.8"
    }

You also can get the plugin from a Bintray repository. In addition to releases this repo contains old and development
versions of the plugin which are not available at the plugin portal. To get the plugin from the Bintray repo, include
the following snippet in your build script:

    buildscript {
       repositories {
           mavenCentral()
           maven {
               url  "https://dl.bintray.com/jetbrains/kotlin-native-dependencies"
           }
       }
    
       dependencies {
           classpath "org.jetbrains.kotlin:kotlin-native-gradle-plugin:0.8-dev-*"
       }
    }
    
    apply plugin: 'konan'

The Kotlin/Native plugin depends on `org.jetbrains.kotlin:kotlin-gradle-plugin`. So if a build contains both these
plugins as buildscript dependencies, it's recommended to **declare them in the same `build.gradle`** to avoid issues with
plugin classpath.

By default the plugin downloads the Kotlin/Native compiler during the first run. If you already downloaded the compiler
manually you may specify the path to its root directory using `konan.home` project property (e.g. in `gradle.properties`).

    konan.home=/home/user/kotlin-native-0.8

In this case the compiler will not be downloaded by the plugin.

## Building artifacts

The Kotlin/Native Gradle plugin allows one to build artifacts of the following types:

* Executable
* KLibrary - a library used by Kotlin/Native compiler (`*.klib`)
* Interoperability library - a special type of library providing an interoperability with some native API. See [`INTEROP.md`](INTEROP.md) for details
* Dynamic library (`*.so`/`*.dylib`/`*.dll`)
* Objective-C framework
* LLVM bitcode

All Kotlin/Native artifacts should be declared in the `konanArtifacts` block. Note that the `konanInterop` script block was removed in
v0.3.4. Use the `interop` method of the `konanArtifact` block instead:

    konanArtifacts {
        program('foo')  // executable 'foo'
        library('bar')  // library 'bar'
        bitcode('baz')  // bitcode file 'baz'
        interop('qux')  // interoperability library 'qux'. Use it instead of konanInterop block.
        dynamic('quux') // dynamic library
        framework ('quuux') // Objective-C framework
	}

All artifacts except interop libraries are built by the Kotlin/Native compiler. Such an artifact may be configured using its script block.
Here one can specify source directories, used libraries and compilation flags (see [**Plugin DSL**](#plugin-dsl) section for details). The plugin
uses `src/main/kotlin/` as a default source directory for all compiler artifacts:

    konanArtifacts {
        // Build foo.klib
        library('foo') {
            srcDir 'src/foo/kotlin' // Use custom source path
        }

        // Build executable 'bar'
        program('bar') {
            // The default source path is used (src/main/kotlin)

            // Optimize the output code
            enableOptimizations true

            libraries {
                // Use foo.klib as a library
                artifact 'foo'
            }
        }
    }

Interop libraries are built using the `cinterop` tool. They also may have configuration blocks but the options available in these blocks
differ from ones available for compiler artifacts. The input for such an artifact is a def-file describing a native API. By default the
def-file path is `src/main/c_interop/<library-name>.def` but it may be changed using the `defFile` method of the configuration block of
an interoperability library:

        konanArtifacts {
            // Interoperability library stdio.klib
            // Use the default def-file path: src/main/c_interop/stdio.def
            interop('stdio')

            // Interoperability library openal.klib
            interop('openal') {
                defFile 'src/openal/openal.def'
            }

            program('main') {
                libraries {
                    // Link with stdio.klib
                    artifact 'stdio'
                }
            }
        }

## Building for different targets

All the artifacts declared in a project may be built for different targets. By default they are built only for `host` target i.e. a
computer used for building. One may change the default  target list using the `konan.targets` project extension:

    konan.targets = [ 'linux', 'android_arm64', 'android_arm32' ]

One may specify a custom target set for each particular artifact using `targets` parameter of an artifact declaration:

    konan.targets = [ 'linux', 'android_arm64' ]

    konanArtifacts {
        // This artifact has no custom targets and will be built
        // for all the default ones: 'linux', 'android_arm64'
        program('foo') { /* ... */ }

        // This artifact will be built only for Linux and Wasm32
        program('bar', targets: ['linux', 'wasm32']) { /* ... */ }

        // An Objective-C framework cannot be built for Linux and Wasm32 thus
        // these targets will be skipped and the artifact will be built only for iOS
        framework('baz', targets: [ 'linux', 'wasm32', 'iphone' ]) { /* ... */ }
    }


The plugin creates tasks to compile each artifact for all targets supported by current host and declared in the `konan.targets` list.
One may perform additional configuration for a target using `target` method of an artifact configuration block:

    konan.targets = [ 'linux', 'macbook', 'wasm32' ]

    konanArtifacts {
        program('foo') {
            // This source file is used for all targets
            srcFiles 'common.kt'

            target('linux') {
                // For Linux common.kt and linux.kt will be compiled
                srcFiles 'linux.kt'
            }

            target('macbook') {
                // For MacOS common.kt and macbook.kt will be compiled
                srcFiles 'macbook.kt'
            }

            // Only common.kt will be compiled for wasm32
        }
    }

One may access to a task for some target via artifact methods or properties:

    // Both of them return a task building artifact 'foo' for MacOS
    konanArtifacts.foo.getByTarget("macbook")
    konanArtifacts.foo.macbook

## Using libraries

One may specify used libraries for artifacts of all types using `libraries` script block:

    program('foo') {
        libraries {
            // configure the libraries used
        }
    }

There are several ways to describe a library used by an artifact:

* Specify a library file directly. One may specify it using the `file` method of the `libraries` script block. All objects accepted by
the [`Project.file`](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:file(java.lang.Object))
method may be passed there:

    ```
    libraries {
        file 'libs/foo.klib'
        files 'lib1.klib', 'lib2.klib'
    }
    ```
* Specify a Kotlin/Native artifact object or its name. In this case the plugin automatically chooses a library with correct target
and set dependencies between building tasks.

    ```
    libraries {
        // Artifact object or just its name may be used
        artifact 'foo'
        artifact kotlinArtifacts.bar
        
        // Artifacts from other projects are also allowed
        artifact project(':bazProject'), 'bazLibrary' 

        // Interopability libraries are also allowed
        // Use it instead of the `useInterop` method available in versions before 0.3.4
        artifact 'stdio'
    }
    ```
* Specify a project containing libraries. In this case all libraries built by the project specified will be used:

    ```
    libraries {
        allLibrariesFrom project(':subproject')

        // If we need only interoperability libraries
        allInteropLibrariesFrom project(':interop')
    }
    ```
* Specify only name of a library. In this case the compiler will look for the library in its repositories.

    ```
    libraries {
        klib 'foo'
        klibs 'lib1', 'lib2'

        // One may specify additional repositories
        // All objects accepted by the Project.file method may be used here
        useRepo 'build/libraries'
    }
    ```

## Multiplatform build

Kotlin/Native, as well as Kotlin/JVM and Kotlin/JS, supports multiplatform projects. Such a support is included in the
Kotlin/Native Gradle plugin by default and there is no need to apply additional plugins to use it. By default
multiplatform support is turned off, and could be enabled with the `enableMultiplatform` DSL method:

    apply 'konan'
    
    konanArtifacts {
        program('foo') {
            enableMultiplatform true
        }
    }

The Gradle plugin adds an `expectedBy` dependency configuration that is used to specify a dependency from Kotlin/Native
project to a common project:

    apply 'konan'
    
    dependencies {
        expectedBy project('commonProject')
    }

When a common project is added as an `expectedBy` dependency, all the artifacts with the multiplatform support enabled
will use it's `main` source set as a common module. One may specify custom source sets for each artifact using the
`commonSourceSets` DSL method. In this case the multiplatform support will be also enabled for this artifact.

    konanArtifacts {
        program('foo') {
            commonSourceSets 'customSourceSet', 'anotherCustomSourceSet'
        }
    }

See more about multiplatform projects [here](https://kotlinlang.org/docs/reference/multiplatform.html).

## Tasks

The Kotlin/Native plugin creates the following tasks:

* __compileKonan\<ArtifactName>\<Target>__. The plugin creates such a task for each target declared in `konan.targets` list and
for each an artifact defined in a `konanArtifacts` block. Such a task may have different properties depending on the artifact type:

    ##### Properties available for a compiler task (executable, library or bitcode building task):

    |Property             |Type                        |Description                                               |
    |---------------------|----------------------------|----------------------------------------------------------|
    |`target             `|`String`                    |Target the artifact is built for. Read only               |
    |`artifactName       `|`String`                    |Base name for the output file (without an extension)      |
    |`destinationDir     `|`File`                      |Directory to place the output artifact                    |
    |`artifact           `|`File`                      |The output artifact. Read only                            |
    |`headerFile         `|`File`                      |The output C header. Only for dynamic libraries, read only|
    |`srcFiles           `|`Collection<FileCollection>`|Compiled files                                            |
    |`nativeLibraries    `|`Collection<FileCollection>`|*.bc libraries used by the artifact                       |
    |`linkerOpts         `|`List<String>`              |Additional options passed to the linker                   |
    |`enableDebug        `|`boolean`                   |Is the debugging support enabled                          |
    |`noStdLib           `|`boolean`                   |Is the artifact not linked with stdlib                    |
    |`noMain             `|`boolean`                   |Is the `main` function provided by a library used         |
    |`enableOptimizations`|`boolean`                   |Are the optimizations enabled                             |
    |`enableAssertions   `|`boolean`                   |Is the assertion support enabled                          |
    |`measureTime        `|`boolean`                   |Does the compiler print phase time                        |
    |`enableMultiplatform`|`boolean`                   |Is multiplatform support enabled for this artifact        |
    |`commonSourceSets`   |`Collection<String>`        |Names of source sets used as a common module              |

    ##### Properties available for a cinterop task (task building an interoperability library):

    |Property        |Type                        |Description                                              |
    |----------------|----------------------------|---------------------------------------------------------|
    |`target        `|`String`                    |Target the artfact is built for. Read only.              |
    |`artifactName  `|`String`                    |Base name for the output file (without an extension)     |
    |`destinationDir`|`File`                      |Directory to place the output artifact                   |
    |`artifact      `|`File`                      |The output artifact. Read only.                          |
    |`defFile       `|`File`                      |Def-file used by the interop                             |
    |`compilerOpts  `|`List<String>`              |Additional options passed to clang                       |
    |`linkerOpts    `|`List<String>`              |Additional options passed to a linker                    |
    |`headers       `|`Collection<FileCollection>`|Additional headers used for stub generation              |
    |`linkFiles     `|`Collection<FileCollection>`|Additional files linked with the stubs                   |


* __compileKonan\<ArtifactName>__. Aggregate task allowing one to build an artifact for several targets. By default it builds
the artifact for all supported targets declared for the project. One may change this behavior by specifying the space-separated
target list in `konan.build.targets` project property:

    ```
    ./gradlew compileKonanFoo -Pkonan.build.targets='android_arm32 android_arm64'
    ```

    The task has no properties to use by a build script.

* __compileKonan__. Aggregate task to build all the Kotlin/Native artifacts for all available targets. `konan.build.targets` project
property also may be used to override the target list. The task has no properties to use by a build script.

* __run\<ArtifactName>__. Such a task is created for each executable supported by current host and allows one to run this
executable. The task is an instance of Gradle's [`Exec`](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Exec.html)
so it supports all settings provided by `Exec`. Additionally, run parameters may be passed to the task using the `runArgs`
project property:

    ```
    ./gradlew runFoo -PrunArgs='foo bar'
    ```

The plugin also edits the default `build` and `clean` tasks so that the first one allows one to build all the artifacts supported
(it's dependent on the `compileKonan` task) and the second one removes the files created by the Kotlin/Native build.

## Building dynamic libraries and frameworks

Kotlin/Native supports building artifacts to be used by other native languages. There are two types of such artifacts:
Objective-C framework and dynamic library.

### Dynamic library

A dynamic library may be built using the `dynamic` artifact block. This block contains the same
options as other ones (except `interop`) allowing one to specify source files, compiler options and libraries used.
Each task building a dynamic library produces two files: the library itself (a `*.so`/`*.dylib`/`*.dll` file depending
on the target platform) and a C language header. Both of them may be accessed via properties of a building task
(both properties have type `File`):
    
    ```
    konanArtifacts {
        // Build a dynamic library 
        dynamic('foo') { /* ... */ }
    }
    
    konanArtifacts.foo.getByTarget('host').artifact    // Points to the library file
    konanArtifacts.foo.getByTarget('host').headerFile  // Points to the header file 
    ```
Using a dynamic library is shown in the [python extension sample](samples/python_extension).
    
### Framework

An Objective-C framework can be built using the `framework` artifact block. This block contains the
same options as other ones. One may access the framework built using `artifact` property of the building task
(see the [**Tasks**](#Tasks) section). Unlike other artifacts this property points to a directory instead of a regular file.
    
    ```
    konanArtifacts {
        // Build an Objective-C framework
        framework('foo') { /* ... */ }
    }
    
    konanArtifacts.foo.getByTarget('host').artifact // Points to the framework directory
    ```
Using a framework is shown in the [calculator sample](samples/calculator).

## Additional options

You can also pass additional command line keys to the compiler or cinterop tool using the `extraOpts` expression
available in artifact configuration script block. For example this sample enables a verbose output for a link and bitcode
generation stages and prints execution time for all compiler phases:

    konanArtifacts {
        program('foo') {
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

## Plugin DSL

    // Default targets to build for.
    konan.targets = ['macbook', 'linux', 'wasm32']

    // Language and API version.
    konan.languageVersion = 'version'
    konan.apiVersion = 'version'
    
    konanArtifacts {
        // Targets to build this artifact for (optional, override the konan.targets list)
        program('foo', targets: ['android_arm32', 'android_arm64']) {

            // Directory with source files. The default path is src/main/kotlin.
            srcDir 'src/other'

            // Source files.
            srcFiles project.fileTree('src')
            srcFiles 'foo.kt', 'bar.kt'

            // Custom output artifact name (without extension).
            // The default is a name of the artifact
            artifactName 'customName'

            // Base Directory for the output artifacts.
            // Separate subdirectories for each target will be created
            // The default is build/konan/bin
            baseDir 'path/to/output/dir'

            libraries {
                // Library files
                file 'foo.klib'
                files 'file1.klib', file2.klib
                files project.files('file3.klib', 'file4.klib')

                // An artifact from the current project
                artifact konanArtifacts.bar
                artifact 'baz'

                // An artifact from another project
                artifact project(':path:to:a:project'), 'artifcatName'

                // All libraries from anohter project
                allLibrariesFrom project(':some:project')

                // Only interoperability libraries from anohter project
                allInteropLibrariesFrom project(':some:interop:project')

                // Named libraries for search in repositoreis
                klib 'foo'
                klib 'bar', 'baz'

                // Custom repository paths
                useRepo 'path/to/a/repo'
                useRepos 'another/repo/1', 'another/repo/2'

            }

            // A naitve library (*.bc) for linking.
            nativeLibrary project.file('path/to/native/library.bc')
            nativeLibraries 'library1.bc', 'library2.bc'

            noStdLib true             // Don't link with stdlib (true/false).
            enableOptimizations true  // Enable compiler optimizations (true/false).
            enableAssertions true     // Enable assertions in binaries generated (true/false).
            enableDebug true          // Enable debugging for binaries generated (true/false).
            noDefaultLibs true        // Don't link with default libraries

            // Custom entry point
            entryPoint 'org.demo.foo'

            // Arguments to be passed to a linker.
            linkerOpts 'Some linker option', 'More linker options'

            // Print all parameters during the build.
            dumpParameters true

            // Print time of compilation phases (equivalent of the `--time` command line option).
            measureTime true

            // Add the `anotherTask` to the compilation task dependencies.
            dependsOn anotherTask

            // Pass additional command line options to the compiler.
            extraOpts '--time', '--verbose', 'linker'

            // Additional configuration for Linux.
            target('linux') {
                // Exact output directory for a file compile for the given target
                // The default is <baseDir>/<target>
                destinationDir 'exact/output/path'

                // Also all options described for this artifact above are available here.
            }
        }

        library('bar') {
            // Library has the same parameters as an executable
            // The default baseDir is build/konan/libs
        }

        bitcode('baz') {
            // Bitcode has the same parameters as an executable
            // The default baseDir is build/konan/bitcode
        }

        dynamic('quux') {
            // Dynamic library has the same parameters as an executable
            // The default baseDir is build/konan/bin
        }

        framework('quuux') {
            // Framework has the same parameters as an executable
            // The default baseDir is build/konan/bin
        }

        interop('qux') {
            // Def-file describing the native API.
            // The default path is src/main/c_interop/<interop-name>.def
            defFile project.file("deffile.def")

            // Custom output artifact name (without extension).
            // The default is a name of the artifact
            artifactName 'customName'

            // Base Directory for the output artifacts.
            // Separate subdirectories for each target will be created
            // The default is build/konan/libs
            baseDir 'path/to/output/dir'

            // Package to place the Kotlin API generated.
            packageName 'org.sample'

            libraries {
                // All library options from the executable example above are available here.
            }

            // Options to be passed to compiler and linker by cinterop tool.
            compilerOpts 'Options for native stubs compilation'
            linkerOpts 'Options for native stubs'

             // Additional headers to parse.
            headers project.files('header1.h', 'header2.h')

            // Directories to look for headers.
            includeDirs {
                // All objects accepted by the Project.file method may be used with both options.

                // Directories for header search (an analogue of the -I<path> compiler option).
                allHeaders 'path1', 'path2'

                // Additional directories to search headers listed in the 'headerFilter' def-file option.
                // -headerFilterAdditionalSearchPrefix command line option analogue.
                headerFilterOnly 'path1', 'path2'
            }
            // A shortcut for includeDirs.allHeaders.
            includeDirs "include/directory" "another/directory"

            // Additional files to link with native stubs.
            link <files which will be linked with native stubs>

            // Print all parameters during the build.
            dumpParameters true

            // Add the `anotherTask` to the stub generation task dependencies.
            dependsOn anotherTask

            // Pass additional command line options to the cinterop tool.
            extraOpts '-shims', 'true'

            // Additional configuration for Linux.
            target('linux') {
                // Exact output directory for a file compile for the given target
                // The default is <baseDir>/<target>
                destinationDir 'exact/output/path'

                // Also all options described for this artifact above are available here.
            }
        }
    }

## Multiplatform DSL

    apply plugin: 'konan'
    
    // In this example common code is located in 'foo' and 'bar' source sets of ':common' project.

    konanArtifacts {
        // All the artifact types except interop libraries may use common modules.
        program('foo') {
            // All artifact settings described above are available here.
            
            // Enable multiplatform support for this artifact.
            enableMultiplatform true
            
            // Set a custom names for source sets used as a common module.
            // The default source set is 'main'
            commonSourceSets 'foo', 'bar'
        }
    }

    dependencies {
        // Use the ':foo' project as a common project for multiplatform build.
        expectedBy project(':common')
    }

## Publishing to Maven.

Publishing the Kotlin/Native artifacts depends on mechanisms which was introduced in Gradle Native support, e.g. Gradle's 
metadata feature, thus there are some additional steps are required. First of all gradle version it shouldn't be less 
then gradle version of kotlin native plugin depends on (currently Gradle 4.7). before Gradle 5.0 feature 
[GRADLE_METADATA](https://github.com/gradle/gradle/blob/master/subprojects/docs/src/docs/design/gradle-module-metadata-specification.md) 
should be enabled for build. e.g. in settings.gradle
````
enableFeaturePreview('GRADLE_METADATA')
````

Some maven repositories requires some declarations in `pom` files, that should be presents in all auxilary `pom` files (
platform x build types). To meet this requirement the Kotlin/Native plugin has following syntax to do it:

 ````
 konanArtifacts {
     interop('libcurl') {
         target('linux') {
             includeDirs.headerFilterOnly '/usr/include'
         }
         target('macbook') {
             includeDirs.headerFilterOnly '/opt/local/include', '/usr/local/include'
         }
         pom {
             withXml {
                 def root = asNode()
                 root.appendNode('name', 'libcurl interop library')
                 root.appendNode('description', 'A library providing interoperability with host libcurl')
             }
         }
     }
 }

 ````
 In this example `name` and `description` tags will be added to each generated `pom` file for _libcurl_ published artifact.