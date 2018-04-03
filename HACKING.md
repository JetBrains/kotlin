 ## Compiler Gradle options

There are several gradle flags one can use for Konan build.

* **-Pbuild_flags** passes flags to the compiler used to build stdlib

        ./gradlew -Pbuild_flags="--disable lower_inline --print_ir" stdlib

* **-Pshims** compiles LLVM interface with tracing "shims". Allowing one 
    to trace the LLVM calls from the compiler.
    Make sure to rebuild the project.

        ./gradlew -Pshims=true dist

* **-Pfilter** allows one to choose test files to run.

        ./gradlew -Pfilter=overflowLong.kt run_external

* **-Pprefix** allows one to choose external test directories to run. Only tests from directories with given prefix will be executed.

        ./gradlew -Pprefix=external_codegen_box_cast run_external

 ## Compiler environment variables

* **KONAN_DATA_DIR** changes `.konan` local data directory location (`$HOME/.konan` by default). Works both with cli compiler and gradle plugin

 ## Testing

To run blackbox compiler tests from JVM Kotlin use (takes time):

    ./gradlew run_external

To update the blackbox compiler tests set TeamCity build number in `gradle.properties`:

    testDataVersion=<build number>:id

and run `./gradlew update_external_tests`

* **-Ptest_flags** passes flags to the compiler used to compile tests

        ./gradlew -Ptest_flags="--time" backend.native:tests:array0

* **-Ptest_target** specifies cross target for a test run. 

        ./gradlew -Ptest_target=raspberrypi backend.native:tests:array0

* **-Premote=user@host** sets remote test execution login/hostname. Good for cross compiled tests.

        ./gradles -Premote=kotlin@111.22.33.444 backend.native:tests:run

