## Profiling the compiler

### Profiling with Async profiler

 IDEA Ultimate contains an Async sampling profiler.
 As of IDEA 2018.3 Async sampling profiler is still an experimental feature, so use Ctrl-Alt-Shift-/ on Linux,
Cmd-Alt-Shift-/ on macOS to activate it. Then start compilation in CLI with `--no-daemon` and
`-Porg.gradle.workers.max=1` flags (running Gradle task with the profiler doesn't seem to work properly) and attach
to the running process using "Run/Attach Profiler to Local Process" menu item.

 Select "K2NativeKt" or "org.jetbrains.kotlin.cli.utilities.MainKt" process.
On completion profiler will produce flame diagram which could be navigated with the mouse
(click-drag moves, wheel scales). More RAM in IDE (>4G) could be helpful when analyzing longer runs.
As Async is a sampling profiler, to get sensible coverage longer runs are important.

### Profiling with YourKit

Unlike Async profiler in IDEA, YourKit can work as an exact profiler and provide complete coverage
of all methods along with exact invocation counters.

Install the YourKit profiler for your platform from https://www.yourkit.com/java/profiler.
Set AGENT variable to the JVMTI agent provided by YourKit, like

        export AGENT=/Applications/YourKit-Java-Profiler-2018.04.app/Contents/Resources/bin/mac/libyjpagent.jnilib

To profile standard library compilation:

        ./gradlew -PstdLibJvmArgs="-agentpath:$AGENT=probe_disable=*,listen=all,tracing"  dist

To profile platform libraries start build of proper target like this:

        ./gradlew -PplatformLibsJvmArgs="-agentpath:$AGENT=probe_disable=*,listen=all,tracing"  ios_arm64PlatformLibs

To profile standalone code compilation use:

        JAVA_OPTS="-agentpath:$AGENT=probe_disable=*,listen=all,tracing" ./dist/bin/konanc file.kt

Then attach to the desired application in YourKit GUI and use CPU tab to inspect CPU consuming methods.
Saving the trace may be needed for more analysis. Adjusting `-Xmx` in `$HOME/.yjp/ui.ini` could help
with the big traces.

To perform memory profiling follow the steps above, and after attachment to the running process
use "Start Object Allocation Recording" button. See https://www.yourkit.com/docs/java/help/allocations.jsp for more details.

## Compiler Gradle options

There are several gradle flags one can use for Konan build.

* **-Pbuild_flags** passes flags to the compiler used to build stdlib

        ./gradlew -Pbuild_flags="--disable lower_inline --print_ir" stdlib

* **-Pshims** compiles LLVM interface with tracing "shims". Allowing one 
    to trace the LLVM calls from the compiler.
    Make sure to rebuild the project.

        ./gradlew -Pshims=true dist

 ## Compiler environment variables

* **KONAN_DATA_DIR** changes `.konan` local data directory location (`$HOME/.konan` by default). Works both with cli compiler and gradle plugin

 ## Testing

To run blackbox compiler tests from JVM Kotlin use (takes time):

    ./gradlew run_external

To update the blackbox compiler tests set TeamCity build number in `gradle.properties`:

    testKotlinVersion=<build number>

* **-Pfilter** allows one to choose test files to run.

        ./gradlew -Pfilter=overflowLong.kt run_external

* **-Pprefix** allows one to choose external test directories to run. Only tests from directories with given prefix will be executed.

        ./gradlew -Pprefix=build_external_compiler_codegen_box_cast run_external

* **-Ptest_flags** passes flags to the compiler used to compile tests

        ./gradlew -Ptest_flags="--time" backend.native:tests:array0

* **-Ptest_target** specifies cross target for a test run. 

        ./gradlew -Ptest_target=raspberrypi backend.native:tests:array0

* **-Premote=user@host** sets remote test execution login/hostname. Good for cross compiled tests.

        ./gradlew -Premote=kotlin@111.22.33.444 backend.native:tests:run

* **-Ptest_verbose** enables printing compiler args and other helpful information during a test execution.

        ./gradlew -Ptest_verbose :backend.native:tests:mpp_optional_expectation
        
* **-Ptest_two_stage** enables two-stage compilation of tests. If two-stage compilation is enabled, test sources are compiled into a klibrary
and then a final native binary is produced from this klibrary using the -Xinclude compiler flag.

        ./gradlew -Ptest_two_stage backend.native:tests:array0
       
 ## Performance measurement
  
 Firstly, it's necessary to build analyzer tool to have opportunity to compare different performance results:
 
    cd tools/benchmarksAnalyzer
    ../../gradlew build
    
 To measure performance of Kotlin/Native compiler on existing benchmarks:
 
    ./gradlew :performance:konanRun

 **NOTE**: **konanRun** task needs built compiler and libs. To test against working tree make sure to run

    ./gradlew dist distPlatformLibs

 before **konanRun**
    
 **konanRun** task can be run separately for one/several benchmark applications:
 
    ./gradlew :performance:cinterop:konanRun
    
 **konanRun** task has parameter `filter` which allows to run only some subset of benchmarks:
 
    ./gradlew :performance:cinterop:konanRun --filter=struct,macros
    
 Or you can use `filterRegex` if you want to specify the filter as regexes:
 
    ./gradlew :performance:ring:konanRun --filterRegex=String.*,Loop.*
    
 There us also verbose mode to follow progress of running benchmarks
 
    ./gradlew :performance:cinterop:konanRun --verbose
    
    > Task :performance:cinterop:konanRun
    [DEBUG] Warm up iterations for benchmark macros
    [DEBUG] Running benchmark macros
    ...
    
 There are also tasks for running benchmarks on JVM (pay attention, some benchmarks e.g. cinterop benchmarks can't be run on JVM)
 
    ./gradlew :performance:jvmRun
    
 Files with results of benchmarks run are saved in `performance/build/nativeReport.json` for konanRun and `jvmReport.json` for jvmRun.
 You can change the output filename by setting the `nativeJson` property for konanRun and `jvmJson` for jvmRun:

    ./gradlew :performance:ring:konanRun --filter=String.*,Loop.* -PnativeJson=stringsAndLoops.json

 You can use the `compilerArgs` property to pass flags to the compiler used to compile the benchmarks:

    ./gradlew :performance:konanRun -PcompilerArgs="--time -g"

 To compare different results run benchmarksAnalyzer tool:
 
    cd tools/benchmarksAnalyzer/build/bin/<target>/benchmarksAnalyzerReleaseExecutable/
    ./benchmarksAnalyzer.kexe <file1> <file2>
    
 Tool has several renders which allow produce output report in different forms (text, html, etc.). To set up render use flag `--render/-r`.
 Output can be redirected to file with flag `--output/-o`.
 To get detailed information about supported options, please use `--help/-h`.
 
 Analyzer tool can compare both local files and files placed on Artifactory/TeamCity.
 
 File description stored on Artifactory
 
    artifactory:<build number>:<target (Linux|Windows10|MacOSX)>:<filename>
    
 Example
    
    artifactory:1.2-dev-7942:Windows10:nativeReport.json
    
 File description stored on TeamCity
  
     teamcity:<build locator>:<filename>
     
 Example
     
     teamcity:id:42491947:nativeReport.json
     
 Pay attention, user and password information(with flag `-u <username>:<password>`) should be provided to get data from TeamCity.
   
## Composite build and testing

If you have a fix spanning both Kotlin and Kotlin/native workspaces you need to be able to test Kotlin/Native composite build. Here's how to do it manually:

### Have a composite build with the proper Kotlin tag.

Find the version of Kotlin the current native is guaranteed to build with. 
The version is specified in `kotlin-native/gradle.properties`. For example:
```
kotlinVersion=1.3.70-dev-1526
```
Checkout `kotlin` workspace to tag `build-1.3.70-dev-1526`. Make sure its path ends with `.../kotlin`. 
Otherwise issues will arise.
Direct `kotlin-native` build to the kotlin with `kotlinProjectPath` in native's `gradle.properties`.

Now you have the kotlin + kotlin-native combination that is known to build.
Apply your fix on top of both workspaces and run
```
$ ./gradlew dist
```

in `kotlin-native` to check the buildability.

### Testing native

For a quick check use:
```
$ ./gradlew sanity 2>&1 | tee log
```

For a longer, more thorough testing build the complete build. Make sure you are running it on a macOS. 


Have a complete build:

```
$ ./gradlew bundle # includes dist as its part
```

then run two test sets:

```
$ ./gradlew backend.native:tests:run 2>&1 | tee log

$ ./gradlew backend.native:tests:runExternal -Ptest_two_stage=true 2>&1 | tee log

```

## LLVM

See [BUILDING_LLVM.md](BUILDING_LLVM.md) if you want to use your own LLVM distribution
instead of provided one.

Following compiler phases control different parts of LLVM pipeline:
1. `LinkBitcodeDependencies`. Linkage of produced bitcode with runtime and some other dependencies.
2. `BitcodeOptimization`. Running LLVM optimization pipeline.
3. `ObjectFiles`. Compilation of bitcode with Clang.

For example, pass `-Xdisable-phases=BitcodeOptimization` to skip optimization pipeline.
Note that disabling `LinkBitcodeDependencies` or `ObjectFiles` will break compilation pipeline.

By default, compiler takes options for Clang from [konan.properties](konan/konan.properties) file
by combining `clangFlags.<TARGET>` and `clang<Noopt/Opt/Debug>Flags.<TARGET>` properties.
To override this behaviour, one can specify flag `-Xoverride-clang-options=<arg1, ..., argN>`.

Please note:
1. Kotlin Native passes bitcode files to Clang instead of C or C++, so many flags won't work.
2. `-c` should be passed because Kotlin/Native calls linker by itself.

Another useful compiler option is `-Xtemporary-files-dir=<PATH>` which allows
to specify a directory for intermediate compiler artifacts like bitcode and object files.


#### Example 1. Bitcode right after IR to Bitcode translation.
```shell script
konanc main.kt -produce bitcode -o bitcode.bc
```

#### Example 2. Bitcode after LLVM optimizations.
```shell script
konanc main.kt -Xtemporary-files-dir=<PATH> -o <OUTPUT_NAME>
```
`<PATH>/<OUTPUT_NAME>.kt.bc` will contain bitcode after LLVM optimization pipeline.

#### Example 3. Replace predefined LLVM pipeline with Clang options.
```shell script
konanc main.kt -Xdisable-phases=BitcodeOptimization -Xoverride-clang-options=-c,-O2
```
