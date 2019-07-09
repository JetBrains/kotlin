# Instrumentation in Kotlin Preloader

The main purpose of Preloader is to speed up class loading at compiler's startup.
But as a side effect, we got a chance to support instrumenting the compiler's code, which is mainly useful for profiling.

## How to quickly set up instrumentation

To run Preloader with instrumentation, pass ```instrument=...``` on the command line:

```
org.jetbrains.kotlin.preloading.Preloader \
             dist/kotlinc/lib/kotlin-compiler.jar \
             org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
             5000 \
             instrument=out/artifacts/Instrumentation/instrumentation.jar \
             <compiler's command-line args>
```

This example uses an artifact already configured in our project.
In this artifact, what to instrument is configured in the ```org.jetbrains.kotlin.preloading.ProfilingInstrumenterExample``` class.
This is determined by the ```src/META-INF/services/org.jetbrains.kotlin.preloading.instrumentation.Instrumenter``` file (see JavaDoc for ```java.util.ServiceLoader```).

## More structured description

**Instrumenter** is any implementation of ```org.jetbrains.kotlin.preloading.instrumentation.Instrumenter``` interface.

Preloader loads the **first** instrumenter service found on the class path.
Services are provided through the [standard JDK mechanism](https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html).

Every preloaded class is run through the instrumenter. Before exiting the program instrumenter's dump() method is called.
**Note** JDK classes and everything in the Preloader's own class path are not preloaded, thus not instrumented.

The ```instrumentation``` module provides a convenient way to define useful instrumenters:

* Derive your class from ```org.jetbrains.kotlin.preloading.instrumentation.InterceptionInstrumenterAdaptor```
* In this class define public static fields with ```@MethodInterceptor``` annotation

Whatever the type of the field, if it has methods named by the convention defined below, they will be called as follows:
* ```enter.*``` - upon entering the instrumented method
* ```normalReturn.*``` - upon returning normally from the instrumented method (not throwing an exception)
* ```exception.*``` - upon explicitly throwing an exception from the instrumented method
* ```exit.*``` - upon exiting the instrumented method (either return or throw)
* ```dump.*``` - upon program termination, useful to display the results

If any of the methods above, except for ```dump.*```, have parameters, they are treated as follows:
* *no annotation* - this parameter receives the respective parameter of the instrumented method
* ```@This``` - this parameter receives the ```this``` of the instrumented method, or ```null``` if there's no ```this```
* ```@ClassName``` - this parameter receives the name of the class containing the instrumented method, must be a ```String```
* ```@MethodName``` - this parameter receives the name of the instrumented method, must be a ```String```
* ```@MethodDesc``` - this parameter receives the JVM descriptor of the instrumented method, like ```(ILjava/lang/Object;)V```, must be a ```String```
* ```@AllArgs``` - this parameter receives an array of all arguments of the instrumented method, must be of type ```Object[]```

See ```org.jetbrains.kotlin.preloading.ProfilingInstrumenterExample```.
