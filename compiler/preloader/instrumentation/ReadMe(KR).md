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

## 더 구조화된 설명

**Instrumenter**는 ```org.jetbrains.kotlin.preloading.instrumentation.Instrumenter``` 인터페이스의 구현입니다.

Preloader는 클래스 경로에서 찾은 **first** instrumenter 서비스를 받아옵니다.
서비스들은 [standard JDK mechanism](http://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html)를 통해 제공됩니다.

모든 preload된 클래스들은 instrumenter를 통해 실행됩니다. 프로그램이 종료되기 전, instrumenter의 dump()메소드가 호출됩니다.
**Note** JDK 클래스들과 Preloader의 고유한 클래스 경로의 모든것은 preload되지 않습니다. 따라서 instrument되지 않습니다.

```instrumentation``` 모듈은 활용도 높은 instrumenter를 정의하기 위한 간편한 방법을 제공합니다.:

* ```org.jetbrains.kotlin.preloading.instrumentation.InterceptionInstrumenterAdaptor```에서 당신의 클래스를 파생시킵니다.
* 이 클래스에서는 ```@MethodInterceptor``` 주석으로 public static 필드를 정의합니다.

필드의 종류가 무엇이든지, 만약 이것이 아래의 정의된 규칙에 따라 명명된 메소드가 있다면 다음과 같이 호출됩니다:
* ```enter.*``` - instrument된 메소드에 들어갈 때
* ```normalReturn.*``` - instrument된 메소드에서 정상적으로 return될 때 (예외를 throw하지 않음)
* ```exception.*``` - instrument된 메소드에서 명시적으로 예외를 throw할 때
* ```exit.*``` - instrument된 메소드를 종료할 때 (throw나 return을 함)
* ```dump.*``` - 프로그램이 종료될 때, 결과를 보여주기에 유용합니다.

위의 ```dump.*```를 제외한 모든 메소드에서 parameter들을 가지고 있을 때 다음과 같이 처리됩니다:
* *no annotation* - 이 parameter는 instrument된 메소드의 parameter를 각각 받습니다.
* ```@This``` - 이 parameter는 instrument된 메소드의 ```this```, 또는 ```this```가 없다면 ```null```를 받습니다.
* ```@ClassName``` - 이 parameter는 instrument된 메소드를 포함하고 있는 클래스의 이름을 받습니다. 반드시 ```String```이어야 합니다.
* ```@MethodName``` - 이 parameter는 instrument된 메소드의 이름을 받습니다. 반드시 ```String```이어야 합니다.
* ```@MethodDesc``` - 이 parameter는 ```(ILjava/lang/Object;)V```처럼 instrument된 메소드의 JVM descriptor ```(ILjava/lang/Object;)V```를 받습니다. 반드시 ```String```이어야 합니다.
* ```@AllArgs``` - 이 parameter는 instrument된 메소드의 모든 argument들의 배열을 받습니다,  반드시 ```Object[]```이어야 합니다.

```org.jetbrains.kotlin.preloading.ProfilingInstrumenterExample```도 참조하세요.
