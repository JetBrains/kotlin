
There are several gradle flags one can use for Konan build.

* **-Pkonanc_flags** passes flags to the compiler used to build stdlib

        ./gradlew -Pkonanc_flags="-disable lower_inline -print_ir" stdlib

* **-Pshims** compiles LLVM interface with tracing "shims". Allowing one 
    to trace the LLVM calls from the compiler.
    Make sure to rebuild the project.

        ./gradlew -Pshims=true dist

* **-Pfilter** allows one to choose test files to run.

        ./gradlew -Pfilter=overflowLong.kt run_external

