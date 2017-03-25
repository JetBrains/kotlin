 ## Compiler Gradle options

There are several gradle flags one can use for Konan build.

* **-Pkonanc_flags** passes flags to the compiler used to build stdlib

        ./gradlew -Pkonanc_flags="--disable lower_inline --print_ir" stdlib

* **-Pshims** compiles LLVM interface with tracing "shims". Allowing one 
    to trace the LLVM calls from the compiler.
    Make sure to rebuild the project.

        ./gradlew -Pshims=true dist

* **-Pfilter** allows one to choose test files to run.

        ./gradlew -Pfilter=overflowLong.kt run_external

* **-Pprefix** allows one to choose external test directories to run. Only tests from directories with given prefix will be executed.

        ./gradlew -Pprefix=external_codegen_box_cast run_external


 ## Testing

To run blackbox compiler tests from JVM Kotlin use (takes time):

    ./gradlew run_external

To update the blackbox compiler tests set TeamCity build number in `gradle.properties`:

    testDataVersion=<build number>:id

and run `./gradlew update_external_tests`