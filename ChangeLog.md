## 1.3.21

### Compiler

#### Fixes

- [`KT-29475`](https://youtrack.jetbrains.com/issue/KT-29475) IllegalArgumentException at getAbstractTypeFromDescriptor with deeply nested expression inside function named with a right parenthesis
- [`KT-29479`](https://youtrack.jetbrains.com/issue/KT-29479) WARN: Could not read file on Java classes from JDK 11+
- [`KT-29360`](https://youtrack.jetbrains.com/issue/KT-29360) Kotlin 1.3.20-eap-100: This marker function should never been called. Looks like compiler did not eliminate it properly. Please, report an issue if you caught this exception.

### IDE

#### Fixes

- [`KT-29486`](https://youtrack.jetbrains.com/issue/KT-29486) Throwable: "Could not find correct module information" through IdeaKotlinUastResolveProviderService.getBindingContext() and ReplaceWithAnnotationAnalyzer.analyzeOriginal()
- [`KT-29394`](https://youtrack.jetbrains.com/issue/KT-29394) Kotlin 1.3.20 EAP: Excess log messages with `kotlin.parallel.tasks.in.project=true`
- [`KT-29474`](https://youtrack.jetbrains.com/issue/KT-29474) Regression in 1.3.20: Kotlin IDE plugin parses all *.gradle.kts files when any class in buildSrc is opened
- [`KT-29290`](https://youtrack.jetbrains.com/issue/KT-29290) Warning "function returning deferred with a name that does not end with async" should not be displayed for let/also/apply...
- [`KT-29494`](https://youtrack.jetbrains.com/issue/KT-29494) Don't report BooleanLiteralArgumentInspection in batch (offline) mode with INFORMATION severity
- [`KT-29525`](https://youtrack.jetbrains.com/issue/KT-29525) turning on parallel tasks causes java.lang.NoClassDefFoundError: Could not initialize class kotlin.Unit sometimes
- [`KT-27769`](https://youtrack.jetbrains.com/issue/KT-27769) Change the DSL marker icon
- [`KT-29118`](https://youtrack.jetbrains.com/issue/KT-29118) Log polluted with multiple "Kotlin does not support alternative resolve" reports

### IDE. Multiplatform

- [`KT-28128`](https://youtrack.jetbrains.com/issue/KT-28128) MPP Kotlin/Native re-downloads POM files on IDE Gradle refresh

### IDE. REPL

- [`KT-29400`](https://youtrack.jetbrains.com/issue/KT-29400) IDE REPL in Gradle project: "IllegalStateException: consoleView must not be null" on module build

### Libraries

#### Fixes

- [`KT-29612`](https://youtrack.jetbrains.com/issue/KT-29612) jlink refuses to consume stdlib containing non-public package kotlin.native

### Tools. CLI

- [`KT-29596`](https://youtrack.jetbrains.com/issue/KT-29596) "AssertionError: Cannot load extensions/common.xml from kotlin-compiler.jar" on IBM JDK 8

### Tools. Gradle

- [`KT-29476`](https://youtrack.jetbrains.com/issue/KT-29476) 1.3.20 MPP Android publishing common api configuration with runtime scope
- [`KT-29725`](https://youtrack.jetbrains.com/issue/KT-29725) MPP Gradle 5.2: NoSuchMethodError in WrapUtil
- [`KT-29485`](https://youtrack.jetbrains.com/issue/KT-29485) In MPP with Gradle module metadata, POM rewriting does not replace the root module publication with a platform one if the former has a custom artifact ID

### Tools. Scripts

- [`KT-29490`](https://youtrack.jetbrains.com/issue/KT-29490) Regression in 1.3.20: Kotlin Jsr223 script engine cannot handle functional return types

### Tools. Kapt

- [`KT-29481`](https://youtrack.jetbrains.com/issue/KT-29481) Annotation processors run on androidTest source set even without the kaptAndroidTest declaration
- [`KT-29513`](https://youtrack.jetbrains.com/issue/KT-29513) kapt throws "ZipException: zip END header not found", when Graal SVM jar in classpath

## 1.3.20

### Android

- [`KT-22571`](https://youtrack.jetbrains.com/issue/KT-22571) Android: Configure Kotlin should add implementation dependency instead of compile

### Compiler

#### New Features

- [`KT-14416`](https://youtrack.jetbrains.com/issue/KT-14416) Support of @PolymorphicSignature in Kotlin compiler
- [`KT-22704`](https://youtrack.jetbrains.com/issue/KT-22704) Allow expect annotations with actual typealias to Java to have default argument values both in expected and in actual
- [`KT-26165`](https://youtrack.jetbrains.com/issue/KT-26165) Support VarHandle in JVM codegen
- [`KT-26999`](https://youtrack.jetbrains.com/issue/KT-26999) Inspection for unused main parameter in Kotlin 1.3

#### Performance Improvements

- [`KT-16867`](https://youtrack.jetbrains.com/issue/KT-16867) Proguard can't unbox Kotlin enums to integers
- [`KT-23466`](https://youtrack.jetbrains.com/issue/KT-23466) kotlin compiler opens-reads-closes .class files many times over
- [`KT-25613`](https://youtrack.jetbrains.com/issue/KT-25613) Optimise boxing of inline class values inside string templates

#### Fixes

- [`KT-2680`](https://youtrack.jetbrains.com/issue/KT-2680) JVM backend should generate synthetic constructors for enum entries (as javac does).
- [`KT-6574`](https://youtrack.jetbrains.com/issue/KT-6574) Enum entry classes should be compiled to package private classes
- [`KT-8341`](https://youtrack.jetbrains.com/issue/KT-8341) Local variable cannot have type parameters
- [`KT-14529`](https://youtrack.jetbrains.com/issue/KT-14529) JS: annotations on property accessors are not serialized
- [`KT-15453`](https://youtrack.jetbrains.com/issue/KT-15453) Annotations are ignored on accessors of private properties
- [`KT-18053`](https://youtrack.jetbrains.com/issue/KT-18053) Unexpected behavior with "in" infix operator and ConcurrentHashMap
- [`KT-18592`](https://youtrack.jetbrains.com/issue/KT-18592) Compiler cannot resolve trait-based superclass of Groovy dependency
- [`KT-19613`](https://youtrack.jetbrains.com/issue/KT-19613) "Public property exposes its private type" not reported for primary constructor properties
- [`KT-20344`](https://youtrack.jetbrains.com/issue/KT-20344) Unused private setter created for property
- [`KT-21862`](https://youtrack.jetbrains.com/issue/KT-21862) java.lang.NoSuchFieldError when calling isInitialized on a lateinit "field" of a companion object
- [`KT-21946`](https://youtrack.jetbrains.com/issue/KT-21946) Compilation error during default lambda inlining when it returns anonymous object
- [`KT-22154`](https://youtrack.jetbrains.com/issue/KT-22154) Warning: Stripped invalid locals information from 1 method when compiling with D8
- [`KT-23369`](https://youtrack.jetbrains.com/issue/KT-23369) Internal compiler error in SMAPParser.parse
- [`KT-23543`](https://youtrack.jetbrains.com/issue/KT-23543) Back-end (JVM) Internal error: Couldn't inline method
- [`KT-23739`](https://youtrack.jetbrains.com/issue/KT-23739) CompilationException: Back-end (JVM) Internal error: Couldn't inline method call: Unmapped line number in inlined function
- [`KT-24156`](https://youtrack.jetbrains.com/issue/KT-24156) For-loop optimization should not be applied in case of custom iterator
- [`KT-24672`](https://youtrack.jetbrains.com/issue/KT-24672) JVM BE: Wrong range is generated in LVT for variables with "late" assignment
- [`KT-24780`](https://youtrack.jetbrains.com/issue/KT-24780) Recursive suspend local functions: "Expected an object reference, but found ."
- [`KT-24937`](https://youtrack.jetbrains.com/issue/KT-24937) Exception from parser (EA-76217)
- [`KT-25058`](https://youtrack.jetbrains.com/issue/KT-25058) Fix deprecated API usage in RemappingClassBuilder
- [`KT-25288`](https://youtrack.jetbrains.com/issue/KT-25288) SOE when inline class is recursive through type parameter upper bound
- [`KT-25295`](https://youtrack.jetbrains.com/issue/KT-25295) “Couldn't transform method node” error on compiling inline class with inherited interface method call
- [`KT-25424`](https://youtrack.jetbrains.com/issue/KT-25424) No coercion to Unit when type argument specified explicitly
- [`KT-25702`](https://youtrack.jetbrains.com/issue/KT-25702) @JvmOverloads should not be allowed on constructors of annotation classes
- [`KT-25893`](https://youtrack.jetbrains.com/issue/KT-25893) crossinline suspend function leads to IllegalStateException: call to 'resume' before 'invoke' with coroutine or compile error
- [`KT-25907`](https://youtrack.jetbrains.com/issue/KT-25907) "Backend Internal error" for a nullable loop variable with explicitly declared type in a for-loop over String
- [`KT-25922`](https://youtrack.jetbrains.com/issue/KT-25922) Back-end Internal error : Couldn't inline method : Lambda inlining : invoke(Continuation) : Trying to access skipped parameter
- [`KT-26126`](https://youtrack.jetbrains.com/issue/KT-26126) Front-end doesn't check that fun with contract and `callsInPlace` effect is an inline function; compiler crashes on val initialization
- [`KT-26366`](https://youtrack.jetbrains.com/issue/KT-26366) UseExperimental with full qualified reference to marker annotation class is reported as error
- [`KT-26384`](https://youtrack.jetbrains.com/issue/KT-26384) Compiler crash with nested multi-catch try, finally block and inline function
- [`KT-26505`](https://youtrack.jetbrains.com/issue/KT-26505) Improve error message on missing script base class kotlin.script.templates.standard.ScriptTemplateWithArgs
- [`KT-26506`](https://youtrack.jetbrains.com/issue/KT-26506) Incorrect bytecode generated for inner class inside inline class referencing outer 'this'
- [`KT-26508`](https://youtrack.jetbrains.com/issue/KT-26508) Incorrect accessor generated for private inline class method call from lambda
- [`KT-26509`](https://youtrack.jetbrains.com/issue/KT-26509) Internal compiler error on generating inline class private method call from companion object
- [`KT-26554`](https://youtrack.jetbrains.com/issue/KT-26554) VerifyError: Bad type on operand stack for inline class with default parameter of underlying type
- [`KT-26582`](https://youtrack.jetbrains.com/issue/KT-26582) Array literal of a primitive wrapper class is loaded as a primitive array literal
- [`KT-26608`](https://youtrack.jetbrains.com/issue/KT-26608) Couldn't inline method call. RuntimeException: Trying to access skipped parameter: Ljava/lang/Object;
- [`KT-26658`](https://youtrack.jetbrains.com/issue/KT-26658) Trying to access skipped parameter exception in code with crossinline suspend lambda with suspend function with default parameter call
- [`KT-26715`](https://youtrack.jetbrains.com/issue/KT-26715) NullPointerException for an inline class constructor reference
- [`KT-26848`](https://youtrack.jetbrains.com/issue/KT-26848) Incorrect line in coroutine debug metadata for first suspension point
- [`KT-26908`](https://youtrack.jetbrains.com/issue/KT-26908) Inline classes can't have a parameter with a default value (Platform declaration clash)
- [`KT-26931`](https://youtrack.jetbrains.com/issue/KT-26931) NSME “InlineClass.foo-impl(LIFace;)I” on calling inherited method from inline class instance
- [`KT-26932`](https://youtrack.jetbrains.com/issue/KT-26932) CCE “Foo cannot be cast to java.lang.String” when accessing underlying value of inline class through reflection
- [`KT-26998`](https://youtrack.jetbrains.com/issue/KT-26998) Default extension fun call in generic Kotlin interface with inline class substituted type of extension receiver fails with internal compiler error
- [`KT-27025`](https://youtrack.jetbrains.com/issue/KT-27025) Inline class access to private companion object fun fails with VerifyError
- [`KT-27070`](https://youtrack.jetbrains.com/issue/KT-27070) Delegated property with inline class type delegate fails with internal error in codegen
- [`KT-27078`](https://youtrack.jetbrains.com/issue/KT-27078) Inline class instance captured in closure fails with internal error (incorrect bytecode generated)
- [`KT-27107`](https://youtrack.jetbrains.com/issue/KT-27107) JvmStatic in inline class companion doesn't generate static method in the class
- [`KT-27113`](https://youtrack.jetbrains.com/issue/KT-27113) Inline class's `toString` is not called when it is used in string extrapolation
- [`KT-27140`](https://youtrack.jetbrains.com/issue/KT-27140) Couldn't inline method call 'ByteArray' with inline class
- [`KT-27162`](https://youtrack.jetbrains.com/issue/KT-27162) Incorrect container is generated to callable reference classes for references to inline class members
- [`KT-27259`](https://youtrack.jetbrains.com/issue/KT-27259) "Internal error: wrong code generated" for nullable inline class with an inline class underlying type
- [`KT-27318`](https://youtrack.jetbrains.com/issue/KT-27318) Interface implementation by delegation to inline class type delegate fails with internal error in codegen
- [`KT-27358`](https://youtrack.jetbrains.com/issue/KT-27358) Boxed inline class type default parameter values fail with CCE at run-time
- [`KT-27416`](https://youtrack.jetbrains.com/issue/KT-27416) "IllegalStateException: Backend Internal error" for inline class with a function with default argument value
- [`KT-27429`](https://youtrack.jetbrains.com/issue/KT-27429) "-java-parameters" compiler argument fails in constructor when there is an inline class parameter present
- [`KT-27513`](https://youtrack.jetbrains.com/issue/KT-27513) Backend Internal Error when using inline method inside inline class
- [`KT-27560`](https://youtrack.jetbrains.com/issue/KT-27560) Executing getter of property with type kotlin.reflect.KSuspendFunction1 throws MalformedParameterizedTypeException
- [`KT-27705`](https://youtrack.jetbrains.com/issue/KT-27705) Internal compiler error (incorrect bytecode generated) when inner class constructor inside inline class references inline class primary val
- [`KT-27706`](https://youtrack.jetbrains.com/issue/KT-27706) Internal compiler error (incorrect bytecode generated) when inner class inside inline class accepts inline class parameter
- [`KT-27732`](https://youtrack.jetbrains.com/issue/KT-27732) Using type inference on platform types corresponding to unsigned types causes compiler error
- [`KT-27737`](https://youtrack.jetbrains.com/issue/KT-27737) CCE for delegated property of inline class type
- [`KT-27762`](https://youtrack.jetbrains.com/issue/KT-27762) The lexer crashes when a vertical tabulation is used
- [`KT-27774`](https://youtrack.jetbrains.com/issue/KT-27774) Update asm to 7.0 in Kotlin backend
- [`KT-27948`](https://youtrack.jetbrains.com/issue/KT-27948) "Argument 2: expected R, but found I" for `equals` operator on nullable and non-null unsigned types
- [`KT-28054`](https://youtrack.jetbrains.com/issue/KT-28054) Inline class: "Cannot pop operand off an empty stack" for calling private secondary constructor from companion object
- [`KT-28061`](https://youtrack.jetbrains.com/issue/KT-28061) Safe call operator and contracts: false negative "A 'return' expression required in a function with a block body"
- [`KT-28185`](https://youtrack.jetbrains.com/issue/KT-28185) Incorrect behaviour of javaClass intrinsic for receivers of inline class type
- [`KT-28188`](https://youtrack.jetbrains.com/issue/KT-28188) CCE when bound callable reference with receiver of inline class type is passed to inline function
- [`KT-28237`](https://youtrack.jetbrains.com/issue/KT-28237) CoroutineStackFrame uses slashes instead of dots in FQN
- [`KT-28361`](https://youtrack.jetbrains.com/issue/KT-28361) Class literal for inline class should return KClass object of the wrapper
- [`KT-28385`](https://youtrack.jetbrains.com/issue/KT-28385) Rewrite at slice FUNCTION in MPP on "red" code
- [`KT-28405`](https://youtrack.jetbrains.com/issue/KT-28405) VE “Bad type on operand stack” at runtime on creating inline class with UIntArray inside
- [`KT-28585`](https://youtrack.jetbrains.com/issue/KT-28585) Inline classes not properly boxed when accessing a `var` (from enclosing scope) from lambda
- [`KT-28847`](https://youtrack.jetbrains.com/issue/KT-28847) Compilation fails with "AssertionError: Rewrite at slice FUNCTOR" on compiling complicated case with delegating property
- [`KT-28879`](https://youtrack.jetbrains.com/issue/KT-28879) "AnalyzerException: Expected I, but found R" when compiling javaClass on inline class value
- [`KT-28920`](https://youtrack.jetbrains.com/issue/KT-28920) "AnalyzerException: Expected I, but found R" when compiling javaObjectType/javaPrimitiveType with inline classes
- [`KT-28965`](https://youtrack.jetbrains.com/issue/KT-28965) Unsound smartcast to definitely not-null if value of one generic type is cast to other generic type
- [`KT-28983`](https://youtrack.jetbrains.com/issue/KT-28983) Wrong mapping of flexible inline class type to primitive type

### IDE

#### New Features

- [`KT-25906`](https://youtrack.jetbrains.com/issue/KT-25906) Kotlin language injection doesn't evaluate constants in string templates
- [`KT-27461`](https://youtrack.jetbrains.com/issue/KT-27461) Provide live template to generate `main()` with no parameters
- [`KT-28371`](https://youtrack.jetbrains.com/issue/KT-28371) Automatically align ?: (elvis operator) after call on the new line

#### Performance Improvements

- [`KT-23738`](https://youtrack.jetbrains.com/issue/KT-23738) Provide stubs for annotation value argument list 
- [`KT-25410`](https://youtrack.jetbrains.com/issue/KT-25410) Opening Settings freezes the UI for 23 seconds
- [`KT-27832`](https://youtrack.jetbrains.com/issue/KT-27832) Improve performance of KotlinGradleProjectResolverExtension
- [`KT-28755`](https://youtrack.jetbrains.com/issue/KT-28755) Optimize searching constructor delegation calls
- [`KT-29297`](https://youtrack.jetbrains.com/issue/KT-29297) Improve performance of light classes in IDE (Java-to-Kotlin interop)

#### Fixes

- [`KT-9840`](https://youtrack.jetbrains.com/issue/KT-9840) Right parenthesis doesn't appear after class name before the colon
- [`KT-13420`](https://youtrack.jetbrains.com/issue/KT-13420) Extend Selection: lambda: whole literal with braces is selected after parameters
- [`KT-17502`](https://youtrack.jetbrains.com/issue/KT-17502) Do not disable "Generate equals and hashCode" actions for data classes
- [`KT-22590`](https://youtrack.jetbrains.com/issue/KT-22590) Create Kotlin SDK if it's absent on importing from gradle/maven Kotlin (JavaScript) projects and on configuring java project to Kotlin(JavaScript), Kotlin(Common)
- [`KT-23268`](https://youtrack.jetbrains.com/issue/KT-23268) IntelliJ plugin: Variables from destructing declarations are not syntax colored as variables
- [`KT-23864`](https://youtrack.jetbrains.com/issue/KT-23864) Copyright message is duplicated in kotlin file in root package after updating copyright
- [`KT-25156`](https://youtrack.jetbrains.com/issue/KT-25156) SOE in IDE on destructuring delegated property declaration
- [`KT-25681`](https://youtrack.jetbrains.com/issue/KT-25681) Remove "Coroutines (experimental)" settings from IDE and do not pass `-Xcoroutines` to JPS compiler (since 1.3)
- [`KT-26868`](https://youtrack.jetbrains.com/issue/KT-26868) MPP: Gradle import: test dependencies get Compile scope
- [`KT-26987`](https://youtrack.jetbrains.com/issue/KT-26987) "Extend Selection" is missing for labeled return
- [`KT-27095`](https://youtrack.jetbrains.com/issue/KT-27095) Kotlin configuration: update EAP repositories to use https instead of http
- [`KT-27321`](https://youtrack.jetbrains.com/issue/KT-27321) Cannot init component state if "internalArguments" presents in xml project structure (kotlinc.xml)
- [`KT-27375`](https://youtrack.jetbrains.com/issue/KT-27375) Kotlin Gradle DSL script: "Unable to get Gradle home directory" in new project with new Gradle wrapper
- [`KT-27380`](https://youtrack.jetbrains.com/issue/KT-27380) `KotlinStringLiteralTextEscaper` returns wrong offset on unparseable elements
- [`KT-27491`](https://youtrack.jetbrains.com/issue/KT-27491) MPP JVM/JS wizard: Use Ktor in the skeleton
- [`KT-27492`](https://youtrack.jetbrains.com/issue/KT-27492) Create some MPP wizard tests
- [`KT-27530`](https://youtrack.jetbrains.com/issue/KT-27530) Kotlin Gradle plugin overwrites the JDK set by jdkName property of the Gradle Idea plugin
- [`KT-27663`](https://youtrack.jetbrains.com/issue/KT-27663) Uast: don't store resolved descriptors in UElements
- [`KT-27907`](https://youtrack.jetbrains.com/issue/KT-27907) Exception on processing auto-generated classes from AS
- [`KT-27954`](https://youtrack.jetbrains.com/issue/KT-27954) Generate -> toString() using "Multiple templates with concatenation" should add spaces after commas
- [`KT-27941`](https://youtrack.jetbrains.com/issue/KT-27941) MPP: Gradle import with "using qualified names" creates 2 modules with the same content root
- [`KT-28199`](https://youtrack.jetbrains.com/issue/KT-28199) Could not get javaResolutionFacade for groovy elements
- [`KT-28348`](https://youtrack.jetbrains.com/issue/KT-28348) Don't log or wrap ProcessCanceledException
- [`KT-28401`](https://youtrack.jetbrains.com/issue/KT-28401) Show parameter info for lambdas during completion
- [`KT-28402`](https://youtrack.jetbrains.com/issue/KT-28402) Automatically indent || and && operators
- [`KT-28458`](https://youtrack.jetbrains.com/issue/KT-28458) New Project Wizard: move multiplatform projects to the new DSL
- [`KT-28513`](https://youtrack.jetbrains.com/issue/KT-28513) Bad Kotlin configuration when old syntax is used for configured Gradle project with >= 4.4 version
- [`KT-28556`](https://youtrack.jetbrains.com/issue/KT-28556) Wrong nullability for @JvmOverloads-generated method parameter in light classes
- [`KT-28997`](https://youtrack.jetbrains.com/issue/KT-28997) Couldn't get delegate for class from any local class or object in script
- [`KT-29027`](https://youtrack.jetbrains.com/issue/KT-29027) Kotlin LightAnnotations don't handle vararg class literals

### IDE. Android

- [`KT-23560`](https://youtrack.jetbrains.com/issue/KT-23560) Scratch: impossible to run scratch file from Android Studio
- [`KT-25450`](https://youtrack.jetbrains.com/issue/KT-25450) NoClassDefFoundError when trying to run a scratch file in Android Studio 3.1.3, Kotlin 1.2.51
- [`KT-26764`](https://youtrack.jetbrains.com/issue/KT-26764) `kotlin` content root isn't generated for Android module of a multiplatform project on Gradle import

### IDE. Code Style, Formatting

- [`KT-5590`](https://youtrack.jetbrains.com/issue/KT-5590) kotlin: line comment must not be on first column by default
- [`KT-24496`](https://youtrack.jetbrains.com/issue/KT-24496) IntelliJ IDEA: Formatting around addition / subtraction not correct for Kotlin
- [`KT-25417`](https://youtrack.jetbrains.com/issue/KT-25417) Incorrect formatting for comments on property accessors
- [`KT-27847`](https://youtrack.jetbrains.com/issue/KT-27847) Destructured declaration continued on the next line is formatted with double indent
- [`KT-28070`](https://youtrack.jetbrains.com/issue/KT-28070) Code style: "Align when multiline" option for "extends / implements list" changes formating of enum constants constructor parameters
- [`KT-28227`](https://youtrack.jetbrains.com/issue/KT-28227) Formatter should not allow enum entries to be on one line with opening brace
- [`KT-28484`](https://youtrack.jetbrains.com/issue/KT-28484) Bad formatting for assignment when continuation for assignments is disabled

### IDE. Completion

- [`KT-18089`](https://youtrack.jetbrains.com/issue/KT-18089) Completion for nullable types without safe call rendered in gray color is barely visible
- [`KT-20706`](https://youtrack.jetbrains.com/issue/KT-20706) KDoc: Unneeded completion is invoked after typing a number/digit in a kdoc comment
- [`KT-22579`](https://youtrack.jetbrains.com/issue/KT-22579) Smart completion should present enum constants with higher rank
- [`KT-23834`](https://youtrack.jetbrains.com/issue/KT-23834) Code completion and auto import do not suggest extension that differs from member only in type parameter
- [`KT-25312`](https://youtrack.jetbrains.com/issue/KT-25312) Autocomplete for overridden members in `expected` class inserts extra `override` word
- [`KT-26632`](https://youtrack.jetbrains.com/issue/KT-26632) Completion: "data class" instead of "data"
- [`KT-27916`](https://youtrack.jetbrains.com/issue/KT-27916) Autocomplete val when auto-completing const

### IDE. Debugger

#### Fixes

- [`KT-13268`](https://youtrack.jetbrains.com/issue/KT-13268) Can't quick evaluate expression with Alt + Click without get operator
- [`KT-14075`](https://youtrack.jetbrains.com/issue/KT-14075) Debugger: Property syntax accesses private Java field rather than synthetic property accessor
- [`KT-22366`](https://youtrack.jetbrains.com/issue/KT-22366) Debugger doesn't stop on certain expressions
- [`KT-23585`](https://youtrack.jetbrains.com/issue/KT-23585) Evaluation of a static interface method call fails
- [`KT-24343`](https://youtrack.jetbrains.com/issue/KT-24343) Debugger, Step Over: IllegalStateException on two consecutive breakpoints when first breakpoint is on an inline function call
- [`KT-24959`](https://youtrack.jetbrains.com/issue/KT-24959) Evaluating my breakpoint condition fails with exception
- [`KT-25667`](https://youtrack.jetbrains.com/issue/KT-25667) Exception in logs from WeakBytecodeDebugInfoStorage (NoStrataPositionManagerHelper)
- [`KT-26795`](https://youtrack.jetbrains.com/issue/KT-26795) Debugger crashes with NullPointerException when evaluating const value in companion object
- [`KT-26798`](https://youtrack.jetbrains.com/issue/KT-26798) Check that step into works with overrides in inline classes
- [`KT-27414`](https://youtrack.jetbrains.com/issue/KT-27414) Use "toString" to render values of inline classes in debugger
- [`KT-27462`](https://youtrack.jetbrains.com/issue/KT-27462) Main without parameters just with inline fun call: Debug: last Step Over can't finish the process
- [`KT-28028`](https://youtrack.jetbrains.com/issue/KT-28028) IDEA is unable to find sources during debugging
- [`KT-28342`](https://youtrack.jetbrains.com/issue/KT-28342) Can't evaluate the synthetic 'field' variable
- [`KT-28487`](https://youtrack.jetbrains.com/issue/KT-28487) ISE “resultValue is null: cannot find method generated_for_debugger_fun” on evaluating value of inline class

### IDE. Decompiler

- [`KT-27284`](https://youtrack.jetbrains.com/issue/KT-27284) Disable highlighting in decompiled Kotlin bytecode
- [`KT-27460`](https://youtrack.jetbrains.com/issue/KT-27460) "Show Kotlin bytecode": "Internal error: null" for an inline extension property from a different file

### IDE. Gradle

- [`KT-27265`](https://youtrack.jetbrains.com/issue/KT-27265) Unresolved reference in IDE on calling JVM source set members of a multiplatform project with Android target from a plain Kotlin/JVM module

### IDE. Gradle. Script

- [`KT-14862`](https://youtrack.jetbrains.com/issue/KT-14862) IDEA links to class file instead of source in buildSrc (Gradle/Kotlin script)
- [`KT-17231`](https://youtrack.jetbrains.com/issue/KT-17231) "Optimize Import" action not working for Gradle script kotlin.
- [`KT-21981`](https://youtrack.jetbrains.com/issue/KT-21981) Optimize imports on the fly does not take implicit imports into account in .kts files
- [`KT-24623`](https://youtrack.jetbrains.com/issue/KT-24623) Class defined in gradle buildSrc folder is marked as unused when it is actually used in Gradle Script Kotlin file
- [`KT-24705`](https://youtrack.jetbrains.com/issue/KT-24705) Script reports are shown in the editor only after caret move
- [`KT-24706`](https://youtrack.jetbrains.com/issue/KT-24706) Do not attach script reports if 'reload dependencies' isn't pressed
- [`KT-25354`](https://youtrack.jetbrains.com/issue/KT-25354) Gradle Kotlin-DSL: Changes of buildSrc are not visible from other modules
- [`KT-25619`](https://youtrack.jetbrains.com/issue/KT-25619) Intentions not working in buildSrc (Gradle)
- [`KT-27674`](https://youtrack.jetbrains.com/issue/KT-27674) Highlighting is skipped in files from buildSrc folder of Gradle project

### IDE. Hints

- [`KT-13118`](https://youtrack.jetbrains.com/issue/KT-13118) Parameter info is not shown for Kotlin last-argument lambdas
- [`KT-25162`](https://youtrack.jetbrains.com/issue/KT-25162) Parameter info for builder functions and lambdas
- [`KT-26689`](https://youtrack.jetbrains.com/issue/KT-26689) Lambda return expression hint not shown when returning a lambda from inside a lambda
- [`KT-27802`](https://youtrack.jetbrains.com/issue/KT-27802) The hint for the if-expression is duplicated inside each branch

### IDE. Inspections and Intentions

#### New Features

- [`KT-2029`](https://youtrack.jetbrains.com/issue/KT-2029) Add inspection for boolean literals passed without using named parameters feature
- [`KT-5071`](https://youtrack.jetbrains.com/issue/KT-5071) Properly surround a function invocation in string template by curly braces
- [`KT-5187`](https://youtrack.jetbrains.com/issue/KT-5187) Quick Fix to remove inline keyword on warning about performance benefits
- [`KT-6025`](https://youtrack.jetbrains.com/issue/KT-6025) Auto-remove toString() call in "Convert concatenation to template"
- [`KT-9983`](https://youtrack.jetbrains.com/issue/KT-9983) "'inline'' modifier is not allowed on virtual members." should have quickfix
- [`KT-12743`](https://youtrack.jetbrains.com/issue/KT-12743) Add Intention to convert nullable var to non-nullable lateinit
- [`KT-15525`](https://youtrack.jetbrains.com/issue/KT-15525) Inspection to warn on thread-blocking invocations from coroutines
- [`KT-17004`](https://youtrack.jetbrains.com/issue/KT-17004) There is no suggestion to add property to supertype
- [`KT-19668`](https://youtrack.jetbrains.com/issue/KT-19668) Inspection "Redundant else in if"
- [`KT-20273`](https://youtrack.jetbrains.com/issue/KT-20273) Inspection to report a setter of a property with a backing field that doesn't update the backing field
- [`KT-20626`](https://youtrack.jetbrains.com/issue/KT-20626) Inspection for '+= creates a new list under the hood'
- [`KT-23691`](https://youtrack.jetbrains.com/issue/KT-23691) Warn about `var` properties with default setter and getter that doesn't reference backing field
- [`KT-24515`](https://youtrack.jetbrains.com/issue/KT-24515) Intention to add an exception under the cursor to @Throws annotations
- [`KT-25171`](https://youtrack.jetbrains.com/issue/KT-25171) Inspection: Change indexed access operator on maps to `Map.getValue`
- [`KT-25620`](https://youtrack.jetbrains.com/issue/KT-25620) Inspection for functions returning Deferred
- [`KT-25718`](https://youtrack.jetbrains.com/issue/KT-25718) Add intention to convert SAM lambda to anonymous object
- [`KT-26236`](https://youtrack.jetbrains.com/issue/KT-26236) QuickFix for ASSIGN_OPERATOR_AMBIGUITY on mutable collection '+=', '-='
- [`KT-26511`](https://youtrack.jetbrains.com/issue/KT-26511) Inspection (without highlighting by default) for unlabeled return inside lambda
- [`KT-26653`](https://youtrack.jetbrains.com/issue/KT-26653) Intention to replace if-else with `x?.let { ... } ?: ...`
- [`KT-26724`](https://youtrack.jetbrains.com/issue/KT-26724) Inspection with a warning for implementation by delegation to a `var` property
- [`KT-26836`](https://youtrack.jetbrains.com/issue/KT-26836) Add quick fix for type mismatch between signed and unsigned types for constant literals
- [`KT-27007`](https://youtrack.jetbrains.com/issue/KT-27007) Intention: add label to return if scope is visually ambiguous
- [`KT-27075`](https://youtrack.jetbrains.com/issue/KT-27075) Add a quick fix/intention to create `expect` member for an added `actual` declaration
- [`KT-27445`](https://youtrack.jetbrains.com/issue/KT-27445) Add quickfix for compiler warning "DEPRECATED_JAVA_ANNOTATION"
- [`KT-28118`](https://youtrack.jetbrains.com/issue/KT-28118) Remove empty parentheses for annotation entries
- [`KT-28631`](https://youtrack.jetbrains.com/issue/KT-28631) Suggest to remove single lambda argument if its name is equal to `it`
- [`KT-28696`](https://youtrack.jetbrains.com/issue/KT-28696) Inspection: detect potentially ambiguous usage of coroutineContext
- [`KT-28699`](https://youtrack.jetbrains.com/issue/KT-28699) Add "Convert to also" intention

#### Performance Improvements

- [`KT-26969`](https://youtrack.jetbrains.com/issue/KT-26969) ConvertCallChainIntoSequence quick fix doesn't use sequences all the way

#### Fixes

- [`KT-4645`](https://youtrack.jetbrains.com/issue/KT-4645) Unexpected behevior of "Replace 'if' with 'when'" intention when called on second or third 'if'
- [`KT-5088`](https://youtrack.jetbrains.com/issue/KT-5088) "Add else branch" quickfix on when should not add braces
- [`KT-7555`](https://youtrack.jetbrains.com/issue/KT-7555) Omit braces when converting 'this' in 'Convert concatenation to template'
- [`KT-8820`](https://youtrack.jetbrains.com/issue/KT-8820) No "Change type" quick fix inside when
- [`KT-8875`](https://youtrack.jetbrains.com/issue/KT-8875) "Remove explicit type" produce red code for extension lambda
- [`KT-12479`](https://youtrack.jetbrains.com/issue/KT-12479) IDEA doesn't propose to replace all usages of deprecated annotation when it declared w/o parentheses
- [`KT-13311`](https://youtrack.jetbrains.com/issue/KT-13311) IDE marks fun finalize() as unused and says that its effective visibility is private
- [`KT-14555`](https://youtrack.jetbrains.com/issue/KT-14555) Strange 'iterate over Nothing' intention
- [`KT-15550`](https://youtrack.jetbrains.com/issue/KT-15550) Intention "Add names to call arguments" isn't available if one argument is a generic function call
- [`KT-15835`](https://youtrack.jetbrains.com/issue/KT-15835) "Leaking 'this' in constructor" for enum class
- [`KT-16338`](https://youtrack.jetbrains.com/issue/KT-16338) "Leaking 'this' in constructor" of non-final class when using 'this::class.java'
- [`KT-20040`](https://youtrack.jetbrains.com/issue/KT-20040) Kotlin Gradle script: unused import doesn't become grey
- [`KT-20725`](https://youtrack.jetbrains.com/issue/KT-20725) Cannot persist excluded methods for inspection "Accessor call that can be replaced with property syntax"
- [`KT-21520`](https://youtrack.jetbrains.com/issue/KT-21520) "Assignment should be lifted out of 'if'" false positive for arguments of different types
- [`KT-23134`](https://youtrack.jetbrains.com/issue/KT-23134) "Remove single lambda parameter" quick fix applied to a lambda parameter with explicit type breaks ::invoke reference on lambda
- [`KT-23512`](https://youtrack.jetbrains.com/issue/KT-23512) "Remove redundant receiver" quick fix makes generic function call incompilable when type could be inferred from removed receiver only
- [`KT-23639`](https://youtrack.jetbrains.com/issue/KT-23639) False positive "Unused symbol" for sealed class type parameters
- [`KT-23693`](https://youtrack.jetbrains.com/issue/KT-23693) `Add missing actual members` quick fix doesn't work if there is already same-named function with the same signature
- [`KT-23744`](https://youtrack.jetbrains.com/issue/KT-23744) "Kotlin library and Gradle plugin versions are different" inspection false positive for non-JVM dependencies
- [`KT-24492`](https://youtrack.jetbrains.com/issue/KT-24492) "Call on collection type may be reduced" does not change labels from mapNotNull to map
- [`KT-25536`](https://youtrack.jetbrains.com/issue/KT-25536) Use non-const Kotlin 'val' usage in Java code isn't reported on case labels (& assignments)
- [`KT-25933`](https://youtrack.jetbrains.com/issue/KT-25933) ReplaceCallWithBinaryOperator should not suggest to replace 'equals' involving floating-point types
- [`KT-25953`](https://youtrack.jetbrains.com/issue/KT-25953) Meaningless auto properties for Atomic classes
- [`KT-25995`](https://youtrack.jetbrains.com/issue/KT-25995) "Simplify comparision" should try to apply "Simplify if expression" when necessary
- [`KT-26051`](https://youtrack.jetbrains.com/issue/KT-26051) False positive "Redundant visibility modifier" for overridden protected property setter made public
- [`KT-26337`](https://youtrack.jetbrains.com/issue/KT-26337) Exception (resource not found) in quick-fix tests in AS32
- [`KT-26481`](https://youtrack.jetbrains.com/issue/KT-26481) Flaky false positive "Receiver parameter is never used" for local extension function
- [`KT-26571`](https://youtrack.jetbrains.com/issue/KT-26571) Too much highlighting from "convert call chain into sequence"
- [`KT-26650`](https://youtrack.jetbrains.com/issue/KT-26650) False negative "Call chain on collection should be converted into 'Sequence'"" on class implementing `Iterable`
- [`KT-26662`](https://youtrack.jetbrains.com/issue/KT-26662) Corner cases around 'this' inside "replace if with safe access"
- [`KT-26669`](https://youtrack.jetbrains.com/issue/KT-26669) "Remove unnecessary parentheses" reports parens of function returned from extension function
- [`KT-26673`](https://youtrack.jetbrains.com/issue/KT-26673) "Remove parameter" quick fix keeps unused type parameter referred in type constraint
- [`KT-26710`](https://youtrack.jetbrains.com/issue/KT-26710) Should not report "implicit 'it' is shadowed" when outer `it` is not used
- [`KT-26839`](https://youtrack.jetbrains.com/issue/KT-26839) Add braces to if statement produces code that is not formatted according to style
- [`KT-26902`](https://youtrack.jetbrains.com/issue/KT-26902) Bad quickfix name for "Call on non-null type may be reduced"
- [`KT-27016`](https://youtrack.jetbrains.com/issue/KT-27016) Replace 'if' with elvis operator w/ interface generates invalid code (breaks type inference)
- [`KT-27034`](https://youtrack.jetbrains.com/issue/KT-27034) "Redundant SAM constructor" inspection shouldn't make all lambda gray (too much highlighting)
- [`KT-27061`](https://youtrack.jetbrains.com/issue/KT-27061) False positive "Convert to secondary constructor" with delegation
- [`KT-27071`](https://youtrack.jetbrains.com/issue/KT-27071) "Add non-null asserted (!!) call" places `!!` at wrong position with operator `get` (array indexing)
- [`KT-27093`](https://youtrack.jetbrains.com/issue/KT-27093) Create actual class from expect class doesn't add all necessary imports
- [`KT-27104`](https://youtrack.jetbrains.com/issue/KT-27104) False positive "Convert call chain into Sequence" with groupingBy
- [`KT-27116`](https://youtrack.jetbrains.com/issue/KT-27116) "Object literal can be converted to lambda" produces code littered with "return@label"
- [`KT-27138`](https://youtrack.jetbrains.com/issue/KT-27138) Change visibility intentions are suggested on properties marked with @JvmField
- [`KT-27139`](https://youtrack.jetbrains.com/issue/KT-27139) Add getter intention is suggested for properties marked with @JvmField
- [`KT-27146`](https://youtrack.jetbrains.com/issue/KT-27146) False positive "map.put() can be converted to assignment" on `super` keyword with `LinkedHashMap` inheritance
- [`KT-27156`](https://youtrack.jetbrains.com/issue/KT-27156) Introduce backing property intention is suggested for property marked with @JvmField
- [`KT-27157`](https://youtrack.jetbrains.com/issue/KT-27157) Convert property to function intention is suggested for property marked with @JvmField
- [`KT-27173`](https://youtrack.jetbrains.com/issue/KT-27173) "Lift return out of `...`" should work on any of targeted `return` keywords
- [`KT-27184`](https://youtrack.jetbrains.com/issue/KT-27184) "Replace with safe call" is not suggested for nullable var property that is impossible to smart cast
- [`KT-27209`](https://youtrack.jetbrains.com/issue/KT-27209) "Loop parameter 'it' is unused": unhelpful quickfix
- [`KT-27291`](https://youtrack.jetbrains.com/issue/KT-27291) "Create" quick fix: "destination directory" field suggests same root and JVM roots for all platforms
- [`KT-27354`](https://youtrack.jetbrains.com/issue/KT-27354) False positive "Make 'Foo' open" for `data` class inheritance
- [`KT-27408`](https://youtrack.jetbrains.com/issue/KT-27408) "Add braces to 'if' statement" moves end-of-line comment inside an `if` branch
- [`KT-27486`](https://youtrack.jetbrains.com/issue/KT-27486) ConvertCallChainIntoSequence quick fix doesn't convert 'unzip' into 'Sequence'
- [`KT-27539`](https://youtrack.jetbrains.com/issue/KT-27539) False positive `Redundant Companion reference` when val in companion is effectively shadowed by inherited val
- [`KT-27584`](https://youtrack.jetbrains.com/issue/KT-27584) False positive "Move lambda argument out of parentheses" when implementing interface by delegation
- [`KT-27590`](https://youtrack.jetbrains.com/issue/KT-27590) No “Change parameter” quick fix for changing argument type from UInt to Int
- [`KT-27619`](https://youtrack.jetbrains.com/issue/KT-27619) Inspection "Invalid property key" should check whether reference is soft or not
- [`KT-27664`](https://youtrack.jetbrains.com/issue/KT-27664) Fix flaky problem in tests "Could not initialize class UnusedSymbolInspection"
- [`KT-27699`](https://youtrack.jetbrains.com/issue/KT-27699) "Remove redundant spread operator" produces incorrect code
- [`KT-27708`](https://youtrack.jetbrains.com/issue/KT-27708) IDE highlights internal constructors used only from Java as unused
- [`KT-27791`](https://youtrack.jetbrains.com/issue/KT-27791) Don't suggest `Implement as constructor parameters` quick fix for `actual` class declaration
- [`KT-27861`](https://youtrack.jetbrains.com/issue/KT-27861) RedundantCompanionReference false positive for nested class with name "Companion"
- [`KT-27906`](https://youtrack.jetbrains.com/issue/KT-27906) SafeCastAndReturn is not reported on code block with unqualified return
- [`KT-27951`](https://youtrack.jetbrains.com/issue/KT-27951) False declaration in actual list (same name but not really actual)
- [`KT-28047`](https://youtrack.jetbrains.com/issue/KT-28047) False positive "Redundant lambda arrow" for lambda returned from `when` branch
- [`KT-28196`](https://youtrack.jetbrains.com/issue/KT-28196) KotlinAddImportAction: AWT events are not allowed inside write action
- [`KT-28200`](https://youtrack.jetbrains.com/issue/KT-28200) KNPE in TypeUtilsKt.getDataFlowAwareTypes
- [`KT-28268`](https://youtrack.jetbrains.com/issue/KT-28268) Don't suggest "make abstract" quick fix for inline classes
- [`KT-28286`](https://youtrack.jetbrains.com/issue/KT-28286) "Unused symbol" inspection: Interface is reported as "class"
- [`KT-28341`](https://youtrack.jetbrains.com/issue/KT-28341) False positive "Introduce backing property" intention for `const` values
- [`KT-28381`](https://youtrack.jetbrains.com/issue/KT-28381) Forbid "move property to constructor" for expect classes
- [`KT-28382`](https://youtrack.jetbrains.com/issue/KT-28382) Forbid "introduce backing property" for expect classes
- [`KT-28383`](https://youtrack.jetbrains.com/issue/KT-28383) Exception during "move to companion" for expect class member
- [`KT-28443`](https://youtrack.jetbrains.com/issue/KT-28443) "Move out of companion object" intention is suggested for @JvmField property inside companion object of interface
- [`KT-28504`](https://youtrack.jetbrains.com/issue/KT-28504) Redundant async inspection: support calls on explicitly given scope
- [`KT-28540`](https://youtrack.jetbrains.com/issue/KT-28540) "Replace assert boolean with assert equality" inspection quickfix doesn't add import statement
- [`KT-28618`](https://youtrack.jetbrains.com/issue/KT-28618) Kotlin: convert anonymous function to lambda expression failed if no space at start of lambda expression
- [`KT-28694`](https://youtrack.jetbrains.com/issue/KT-28694) "Assign backing field" quick fix adds empty line before created assignment
- [`KT-28716`](https://youtrack.jetbrains.com/issue/KT-28716) KotlinDefaultHighlightingSettingsProvider suppresses inspections in non-kotlin files
- [`KT-28744`](https://youtrack.jetbrains.com/issue/KT-28744) val-keyword went missing from constructor of inline class after applying “Create actual class...” intention
- [`KT-28745`](https://youtrack.jetbrains.com/issue/KT-28745) val-keyword went missing from constructor of inline class after applying “Create expected class in common module...” intention

### IDE. KDoc

- [`KT-24788`](https://youtrack.jetbrains.com/issue/KT-24788) Endless exceptions in offline inspections

### IDE. Multiplatform

- [`KT-26518`](https://youtrack.jetbrains.com/issue/KT-26518) `Create actual ...` quick fix doesn't add a primary constructor call for the actual secondary constructor
- [`KT-26893`](https://youtrack.jetbrains.com/issue/KT-26893) Multiplatform projects fail to import into Android Studio 3.3, 3.4
- [`KT-26957`](https://youtrack.jetbrains.com/issue/KT-26957) Merge expect gutter icon, when used for the same line
- [`KT-27295`](https://youtrack.jetbrains.com/issue/KT-27295) MPP: Rebuild module / Recompile source does nothing for Native with Delegate to gradle = Yes
- [`KT-27296`](https://youtrack.jetbrains.com/issue/KT-27296) MPP: Rebuild module / Recompile source does nothing for Common with Delegate to gradle = Yes
- [`KT-27335`](https://youtrack.jetbrains.com/issue/KT-27335) New multiplatform wizard: mobile library is generated with failed test
- [`KT-27595`](https://youtrack.jetbrains.com/issue/KT-27595) KNPE on attempt to generate `equals()`, `hashCode()`, `toString()` for `expect` class

### IDE. Navigation

- [`KT-22637`](https://youtrack.jetbrains.com/issue/KT-22637) Go to actual declarations for enum values should choose correct value if they are written in one line
- [`KT-27494`](https://youtrack.jetbrains.com/issue/KT-27494) Create tooling tests for new-multiplatform
- [`KT-28206`](https://youtrack.jetbrains.com/issue/KT-28206) Go to implementations on expect enum shows not only enum classes, but also all members
- [`KT-28398`](https://youtrack.jetbrains.com/issue/KT-28398) Broken navigation to actual declaration of `println()` in non-gradle project

### IDE. Project View

- [`KT-26210`](https://youtrack.jetbrains.com/issue/KT-26210) IOE “Cannot create file” on creating new file with existing filename by pasting a code in Project view
- [`KT-27903`](https://youtrack.jetbrains.com/issue/KT-27903) Can create file with empty name without any warning

### IDE. REPL

- [`KT-29285`](https://youtrack.jetbrains.com/issue/KT-29285) Starting REPL in Gradle project: Will compile into IDEA's out folder which then shadows Gradle's compile output

### IDE. Refactorings

- [`KT-23603`](https://youtrack.jetbrains.com/issue/KT-23603) Add the support for find usages/refactoring of the buildSrc sources in gradle kotlin DSL build scripts
- [`KT-26696`](https://youtrack.jetbrains.com/issue/KT-26696) Copy, Move: "Destination directory" field does not allow to choose a path from non-JVM module
- [`KT-28408`](https://youtrack.jetbrains.com/issue/KT-28408) "Extract interface" action should not show private properties
- [`KT-28476`](https://youtrack.jetbrains.com/issue/KT-28476) Extract interface / super class on non-JVM class throws KNPE

### IDE. Scratch

- [`KT-23523`](https://youtrack.jetbrains.com/issue/KT-23523) Filter out fake gradle modules from checkbox in Scratch file panel
- [`KT-25032`](https://youtrack.jetbrains.com/issue/KT-25032) Scratch: IDEA hangs/freezes on code that never returns (infinite loops)
- [`KT-26271`](https://youtrack.jetbrains.com/issue/KT-26271) Scratches for Kotlin  do not work when clicking "Run Scratch File" button
- [`KT-26332`](https://youtrack.jetbrains.com/issue/KT-26332) Fix classpath intention in Kotlin scratch file in Java only project doesn't do anything
- [`KT-27628`](https://youtrack.jetbrains.com/issue/KT-27628) Scratch blocks AWT Queue thread
- [`KT-28045`](https://youtrack.jetbrains.com/issue/KT-28045) 'Run kotlin scratch' is shown for jest tests

### IDE. Script

- [`KT-24465`](https://youtrack.jetbrains.com/issue/KT-24465) Provide a UI to manage script definitions
- [`KT-24466`](https://youtrack.jetbrains.com/issue/KT-24466) Add warning when there are multiple script definitions for one script
- [`KT-25818`](https://youtrack.jetbrains.com/issue/KT-25818) IDE Scripting Console files shouldn't have scratch panel
- [`KT-26331`](https://youtrack.jetbrains.com/issue/KT-26331) Please extract ScriptDefinitionContributor/KotlinScriptDefinition from kotlin-plugin.jar to separate jar
- [`KT-27669`](https://youtrack.jetbrains.com/issue/KT-27669) Consider moving expensive tasks out of the UI thread
- [`KT-27743`](https://youtrack.jetbrains.com/issue/KT-27743) Do not start multiple background threads loading dependencies for different scripts
- [`KT-27817`](https://youtrack.jetbrains.com/issue/KT-27817) Implement a lightweight EP in a separate public jar for supplying script definitions to IDEA
- [`KT-27960`](https://youtrack.jetbrains.com/issue/KT-27960) Add capability to import one Script to another
- [`KT-28046`](https://youtrack.jetbrains.com/issue/KT-28046) "Reload script dependencies on file change" option is missing after project restart

### IDE. Tests Support

- [`KT-27977`](https://youtrack.jetbrains.com/issue/KT-27977) Missing 'run' gutter on a test method of an abstract class
- [`KT-28080`](https://youtrack.jetbrains.com/issue/KT-28080) Wrong run configuration created from context for test method in abstract class

### JS. Tools

- [`KT-27361`](https://youtrack.jetbrains.com/issue/KT-27361) Support NamedConstructor in idl2k
- [`KT-28786`](https://youtrack.jetbrains.com/issue/KT-28786) Float values initialized incorrectly while translating from IDL
- [`KT-28821`](https://youtrack.jetbrains.com/issue/KT-28821) Kotlin/JS missing ClipboardEvent definitions
- [`KT-28864`](https://youtrack.jetbrains.com/issue/KT-28864) Better support for TrackEvent, MediaStreamTrackEvent and RTCTrackEvent in idl

### JavaScript

- [`KT-27611`](https://youtrack.jetbrains.com/issue/KT-27611) Calling a suspending function of a JS library causes "Uncaught ReferenceError: CoroutineImpl is not defined"
- [`KT-28207`](https://youtrack.jetbrains.com/issue/KT-28207) Finally block loops forever for specific code shape
- [`KT-28215`](https://youtrack.jetbrains.com/issue/KT-28215) JS: inline suspend function not usable in non-inlined form
- [`KT-29003`](https://youtrack.jetbrains.com/issue/KT-29003) KotlinJS: Size of String in stdlib is limited if the the Constructor String(chars: CharArray) gets used

### Libraries

#### New Features

- [`KT-18398`](https://youtrack.jetbrains.com/issue/KT-18398) Provide a way for libraries to avoid mixing Kotlin 1.0 and 1.1 dependencies in end user projects
- [`KT-27919`](https://youtrack.jetbrains.com/issue/KT-27919) Publish modularized artifacts under 'modular' classifier

#### Performance Improvements

- [`KT-27251`](https://youtrack.jetbrains.com/issue/KT-27251) Do not use Stack in FileTreeWalk iterator implementation

#### Fixes

- [`KT-12473`](https://youtrack.jetbrains.com/issue/KT-12473) KotlinJS - comparator returning 0 changes order
- [`KT-20743`](https://youtrack.jetbrains.com/issue/KT-20743) Use strongly typed events in Kotlin2js DOM API
- [`KT-20865`](https://youtrack.jetbrains.com/issue/KT-20865) Retrieving groups by name is not supported on Java 9 even with `kotlin-stdlib-jre8` in the classpath
- [`KT-23932`](https://youtrack.jetbrains.com/issue/KT-23932) add "PointerEvent" for kotlin-stdlib-js 
- [`KT-24336`](https://youtrack.jetbrains.com/issue/KT-24336) Kotlin/JS missing SVGMaskElement interface
- [`KT-25371`](https://youtrack.jetbrains.com/issue/KT-25371) Support unsigned integers in kotlinx-metadata-jvm
- [`KT-27629`](https://youtrack.jetbrains.com/issue/KT-27629) kotlin.test BeforeTest/AfterTest annotation mapping for TestNG
- [`KT-28091`](https://youtrack.jetbrains.com/issue/KT-28091) Provide correct AbstractMutableCollections declarations in stdlib-common
- [`KT-28251`](https://youtrack.jetbrains.com/issue/KT-28251) Stdlib: Deprecated ReplaceWith `kotlin.math.log` replacement instead of `kotlin.math.ln`
- [`KT-28488`](https://youtrack.jetbrains.com/issue/KT-28488) Add clarification for COROUTINES_SUSPENDED documentation
- [`KT-28572`](https://youtrack.jetbrains.com/issue/KT-28572) readLine() stumbles at surrogate pairs
- [`KT-29187`](https://youtrack.jetbrains.com/issue/KT-29187) JS toTypedArray returns array of invalid type for LongArray and BooleanArray

### Reflection

- [`KT-26765`](https://youtrack.jetbrains.com/issue/KT-26765) Support calling constructors with inline classes in the signature in reflection
- [`KT-27585`](https://youtrack.jetbrains.com/issue/KT-27585) Flaky IllegalPropertyDelegateAccessException: Cannot obtain the delegate of a non-accessible property. Use "isAccessible = true" to make the property accessible
- [`KT-27598`](https://youtrack.jetbrains.com/issue/KT-27598) "KotlinReflectionInternalError" when using `callBy` on constructor that has inline class parameters
- [`KT-27913`](https://youtrack.jetbrains.com/issue/KT-27913) ReflectJvmMapping.getKotlinFunction(ctor) works incorrectly with types containing variables of inline class

### Tools. CLI

- [`KT-27226`](https://youtrack.jetbrains.com/issue/KT-27226) Argfiles: An empty argument in quotes with a whitespace or a newline after it interrupts further reading of arguments
- [`KT-27430`](https://youtrack.jetbrains.com/issue/KT-27430) [Experimental API] Report warning instead of error if non-marker is used in -Xuse-experimental/-Xexperimental
- [`KT-27626`](https://youtrack.jetbrains.com/issue/KT-27626) -Xmodule-path does not work in Gradle project with Java 9
- [`KT-27709`](https://youtrack.jetbrains.com/issue/KT-27709) Using an experimental API that does not exist should warn, not error
- [`KT-27775`](https://youtrack.jetbrains.com/issue/KT-27775) Re-enable directories passed as <sources> in -Xbuild-file
- [`KT-27930`](https://youtrack.jetbrains.com/issue/KT-27930) Do not use toURI in ModuleVisibilityUtilsKt.isContainedByCompiledPartOfOurModule if possible
- [`KT-28180`](https://youtrack.jetbrains.com/issue/KT-28180) Backslash-separated file paths in argfiles do not work on Windows
- [`KT-28974`](https://youtrack.jetbrains.com/issue/KT-28974) Serialization bug in CommonToolArguments, affecting MPP project data serialization

### Tools. Compiler Plugins

- [`KT-24997`](https://youtrack.jetbrains.com/issue/KT-24997) Pass arguments to Kapt in human-readable format
- [`KT-24998`](https://youtrack.jetbrains.com/issue/KT-24998) Introduce separate command line tool specifically for Kapt in order to improve UX
- [`KT-25128`](https://youtrack.jetbrains.com/issue/KT-25128) ABI jar generation in the CLI compiler for Bazel like build systems.

### Tools. Gradle

#### New Features

- [`KT-26963`](https://youtrack.jetbrains.com/issue/KT-26963) Warn user that a custom platform-agnostic source set wouldn't be included into build unless it is required for other source sets
- [`KT-27394`](https://youtrack.jetbrains.com/issue/KT-27394) Add kotlinOptions in compilations of the new MPP model
- [`KT-27535`](https://youtrack.jetbrains.com/issue/KT-27535) Implement AARs building and publishing in new MPP
- [`KT-27685`](https://youtrack.jetbrains.com/issue/KT-27685) In new MPP, expose a compilation's default source set via DSL
- [`KT-28155`](https://youtrack.jetbrains.com/issue/KT-28155) Add ability to run tasks in parallel within project
- [`KT-28842`](https://youtrack.jetbrains.com/issue/KT-28842) Enable JS IC by default

#### Performance Improvements

- [`KT-24530`](https://youtrack.jetbrains.com/issue/KT-24530) Enable compile avoidance for kaptKotlin tasks
- [`KT-28037`](https://youtrack.jetbrains.com/issue/KT-28037) In-process Kotlin compiler leaks thread local values

#### Fixes

- [`KT-26065`](https://youtrack.jetbrains.com/issue/KT-26065) Kotlin Gradle plugin resolves dependencies at configuration time
- [`KT-26389`](https://youtrack.jetbrains.com/issue/KT-26389) Support Gradle Kotlin DSL in projects with the `kotlin-multiplatform` plugin
- [`KT-26663`](https://youtrack.jetbrains.com/issue/KT-26663) Gradle dependency DSL features missing for the new MPP dependencies
- [`KT-26808`](https://youtrack.jetbrains.com/issue/KT-26808) Deprecation Warning Gradle 5 - "DefaultSourceDirectorySet constructor has been deprecated"
- [`KT-26978`](https://youtrack.jetbrains.com/issue/KT-26978) Gradle verification fails on DiscoverScriptExtensionsTask
- [`KT-27682`](https://youtrack.jetbrains.com/issue/KT-27682) Kotlin MPP DSL: a target is missing the `attributes { ... }` function, only the `attributes` property is available.
- [`KT-27950`](https://youtrack.jetbrains.com/issue/KT-27950) Gradle 5.0-rc1: "Compilation with Kotlin compile daemon was not successful"
- [`KT-28355`](https://youtrack.jetbrains.com/issue/KT-28355) Gradle Kotlin plugin publishes "api" dependencies with runtime scope
- [`KT-28363`](https://youtrack.jetbrains.com/issue/KT-28363) Enable resources processing for Kotlin/JS target in multiplatform projects
- [`KT-28469`](https://youtrack.jetbrains.com/issue/KT-28469) Gradle Plugin: Task DiscoverScriptExtensionsTask is never up-to-date
- [`KT-28482`](https://youtrack.jetbrains.com/issue/KT-28482) Always rewrite the MPP dependencies in POMs, even when publishing with Gradle metadata
- [`KT-28520`](https://youtrack.jetbrains.com/issue/KT-28520) MPP plugin can't be applied altogether with the "maven-publish" plugin in a Gradle 5 build
- [`KT-28635`](https://youtrack.jetbrains.com/issue/KT-28635) fromPreset() in MPP Gradle plugin DSL is hard to use from Gradle Kotlin DSL scripts
- [`KT-28749`](https://youtrack.jetbrains.com/issue/KT-28749) Expose `allKotlinSourceSets` in `KotlinCompilation`
- [`KT-28795`](https://youtrack.jetbrains.com/issue/KT-28795) The localToProject attribute is not properly disambiguated with Gradle 4.10.2+
- [`KT-28836`](https://youtrack.jetbrains.com/issue/KT-28836) Kotlin compiler logs from Gradle Kotlin Plugin aren't captured by Gradle
- [`KT-29058`](https://youtrack.jetbrains.com/issue/KT-29058) Gradle Plugin: Multiplatform project with maven-publish plugin does not use project group for "metadata" artifact POM

### Tools. J2K

- [`KT-26073`](https://youtrack.jetbrains.com/issue/KT-26073) Irrelevant "create extra commit with java->kt rename"

### Tools. JPS

- [`KT-26980`](https://youtrack.jetbrains.com/issue/KT-26980) JPS Native warning is duplicated for test source sets
- [`KT-27285`](https://youtrack.jetbrains.com/issue/KT-27285) MPP: invalid common -> platform dependency: JPS fails with Throwable "Cannot initialize Kotlin context: Cyclically dependent modules" at KotlinChunk.<init>()
- [`KT-27622`](https://youtrack.jetbrains.com/issue/KT-27622) JPS, JS: Resources marked as "kotlin-resource" are not copied to the out folder in a Kotlin-js project
- [`KT-28095`](https://youtrack.jetbrains.com/issue/KT-28095) JPS: support -Xcommon-sources for multiplatform projects (JS)
- [`KT-28316`](https://youtrack.jetbrains.com/issue/KT-28316) Report `Native is not yet supported in IDEA internal build system` on JPS build once per project/multiplatform module
- [`KT-28527`](https://youtrack.jetbrains.com/issue/KT-28527) JPS: Serialization plugin not loaded in ktor
- [`KT-28900`](https://youtrack.jetbrains.com/issue/KT-28900) With "Keep compiler process alive between invocations = No" (disabled daemon) JPS rebuild fails with SCE: "Provider AndroidCommandLineProcessor not a subtype" at PluginCliParser.processPluginOptions()

### Tools. Scripts

- [`KT-27382`](https://youtrack.jetbrains.com/issue/KT-27382) Embeddable version of scripting support (KEEP 75)
- [`KT-27497`](https://youtrack.jetbrains.com/issue/KT-27497) kotlin script -  No class roots are found in the JDK path
- [`KT-29293`](https://youtrack.jetbrains.com/issue/KT-29293) Script compilation - standard libs are not added to the dependencies
- [`KT-29301`](https://youtrack.jetbrains.com/issue/KT-29301) Some ivy resolvers are proguarded out of the kotlin-main-kts
- [`KT-29319`](https://youtrack.jetbrains.com/issue/KT-29319) scripts default jvmTarget causes inlining problems - default should be 1.8

### Tools. kapt

#### New Features

- [`KT-28024`](https://youtrack.jetbrains.com/issue/KT-28024) Kapt: Add option for printing timings for individual annotation processors
- [`KT-28025`](https://youtrack.jetbrains.com/issue/KT-28025) Detect memory leaks in annotation processors

#### Performance Improvements

- [`KT-28852`](https://youtrack.jetbrains.com/issue/KT-28852) Cache classloaders for tools.jar and kapt in Gradle workers

#### Fixes

- [`KT-24368`](https://youtrack.jetbrains.com/issue/KT-24368) Kapt: Do not include compile classpath entries in the annotation processing classpath
- [`KT-25756`](https://youtrack.jetbrains.com/issue/KT-25756) Investigate file descriptors leaks in kapt
- [`KT-26145`](https://youtrack.jetbrains.com/issue/KT-26145) Using `kapt` without the `kotlin-kapt` plugin should throw a build error
- [`KT-26304`](https://youtrack.jetbrains.com/issue/KT-26304) Build fails with "cannot find symbol" using gRPC with dagger; stub compilation fails to find classes generated by kapt
- [`KT-26725`](https://youtrack.jetbrains.com/issue/KT-26725) Kapt does not handle androidx.annotation.RecentlyNullable correctly
- [`KT-26817`](https://youtrack.jetbrains.com/issue/KT-26817) kapt 1.2.60+ ignores .java files that are symlinks
- [`KT-27126`](https://youtrack.jetbrains.com/issue/KT-27126) kapt: class implementing List<T> generates bad stub
- [`KT-27188`](https://youtrack.jetbrains.com/issue/KT-27188) kapt Gradle plugin fails in Java 10+ ("Cannot find tools.jar")
- [`KT-27334`](https://youtrack.jetbrains.com/issue/KT-27334) [Kapt] Stub generator uses constant value in method annotation instead of constant name.
- [`KT-27404`](https://youtrack.jetbrains.com/issue/KT-27404) Kapt does not call annotation processors on custom (e.g., androidTest) source sets if all dependencies are inherited from the main kapt configuration
- [`KT-27487`](https://youtrack.jetbrains.com/issue/KT-27487) Previous value is passed to annotation parameter using annotation processing
- [`KT-27711`](https://youtrack.jetbrains.com/issue/KT-27711) kapt: ArrayIndexOutOfBoundsException: 0
- [`KT-27910`](https://youtrack.jetbrains.com/issue/KT-27910) Kapt lazy stub without explicit type that initializes an object expression breaks stubbing

## Previous releases
This release also includes the fixes and improvements from the [previous releases](https://github.com/JetBrains/kotlin/releases/tag/v1.3.11).
