# Kotlin Preloader의 Instrumentation

Preloader의 주요 목적은 컴파일러를 시작하는데 있어 클래스 로딩속도를 올리기 위함입니다.
하지만 부작용으로 주로 프로파일링을 위한 컴파일러 코드 instrumenting을 지원하기 위한 기회를 얻었습니다.

## 빠르게 Instrumentation을 구성하는법

Preloader를 instrumentation과 같이 실행하려면 , command line에 ```instrument=...```을 실행합니다.:

```
org.jetbrains.kotlin.preloading.Preloader \
             dist/kotlinc/lib/kotlin-compiler.jar \
             org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
             5000 \
             instrument=out/artifacts/Instrumentation/instrumentation.jar \
             <compiler's command-line args>
```

이 예제는 우리 프로젝트의 이미 설정된 artifact를 사용한 것입니다.
이 artifact에서, 무엇을 instrument하는지는 ```org.jetbrains.kotlin.preloading.ProfilingInstrumenterExample``` 클래스 설정에서 구성됩니다.
이것은  ```src/META-INF/services/org.jetbrains.kotlin.preloading.instrumentation.Instrumenter``` 파일에 의해 결정됩니다. (```java.util.ServiceLoader```에 대한 JavaDoc을 참조하세요).

## More structured description

**Instrumenter** is any implementation of ```org.jetbrains.kotlin.preloading.instrumentation.Instrumenter``` interface.

Preloader loads the **first** instrumenter service found on the class path.
Services are provided through the [standard JDK mechanism](http://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html).

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
