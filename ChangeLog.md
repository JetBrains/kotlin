## 1.3.20

### Android

- [`KT-22571`](https://youtrack.jetbrains.com/issue/KT-22571) Android: Configure Kotlin should add implementation dependency instead of compile
- [`KT-26341`](https://youtrack.jetbrains.com/issue/KT-26341) Kotlin reference for Android Platform reports Android version instead of API Level number

### Build Infrastructure

- [`KT-28437`](https://youtrack.jetbrains.com/issue/KT-28437) Switch Kotlin to 183 platform

### Compiler

#### New Features

- [`KT-14416`](https://youtrack.jetbrains.com/issue/KT-14416) Support of @PolymorphicSignature in Kotlin compiler
- [`KT-25128`](https://youtrack.jetbrains.com/issue/KT-25128) ABI jar generation in the CLI compiler for Bazel like build systems.
- [`KT-26165`](https://youtrack.jetbrains.com/issue/KT-26165) Support VarHandle in JVM codegen
- [`KT-26999`](https://youtrack.jetbrains.com/issue/KT-26999) Inspection for unused main parameter in Kotlin 1.3

#### Performance Improvements

- [`KT-16867`](https://youtrack.jetbrains.com/issue/KT-16867) Proguard can't unbox Kotlin enums to integers
- [`KT-25613`](https://youtrack.jetbrains.com/issue/KT-25613) Optimise boxing of inline class values inside string templates

#### Fixes

- [`KT-2680`](https://youtrack.jetbrains.com/issue/KT-2680) JVM backend should generate synthetic constructors for enum entries (as javac does).
- [`KT-14529`](https://youtrack.jetbrains.com/issue/KT-14529) JS: annotations on property accessors are not serialized
- [`KT-18053`](https://youtrack.jetbrains.com/issue/KT-18053) Unexpected behavior with "in" infix operator and ConcurrentHashMap
- [`KT-18592`](https://youtrack.jetbrains.com/issue/KT-18592) Compiler cannot resolve trait-based superclass of Groovy dependency
- [`KT-19613`](https://youtrack.jetbrains.com/issue/KT-19613) "Public property exposes its private type" not reported for primary constructor properties
- [`KT-20344`](https://youtrack.jetbrains.com/issue/KT-20344) Unused private setter created for property
- [`KT-23369`](https://youtrack.jetbrains.com/issue/KT-23369) Internal compiler error in SMAPParser.parse
- [`KT-24937`](https://youtrack.jetbrains.com/issue/KT-24937) Exception from parser (EA-76217)
- [`KT-25058`](https://youtrack.jetbrains.com/issue/KT-25058) Fix deprecated API usage in RemappingClassBuilder
- [`KT-25288`](https://youtrack.jetbrains.com/issue/KT-25288) SOE when inline class is recursive through type parameter upper bound
- [`KT-25295`](https://youtrack.jetbrains.com/issue/KT-25295) “Couldn't transform method node” error on compiling inline class with inherited interface method call
- [`KT-25424`](https://youtrack.jetbrains.com/issue/KT-25424) No coercion to Unit when type argument specified explicitly
- [`KT-25893`](https://youtrack.jetbrains.com/issue/KT-25893) crossinline suspend function leads to IllegalStateException: call to 'resume' before 'invoke' with coroutine or compile error
- [`KT-25907`](https://youtrack.jetbrains.com/issue/KT-25907) "Backend Internal error" for a nullable loop variable with explicitly declared type in a for-loop over String
- [`KT-25922`](https://youtrack.jetbrains.com/issue/KT-25922) Back-end Internal error : Couldn't inline method : Lambda inlining : invoke(Continuation) : Trying to access skipped parameter
- [`KT-26366`](https://youtrack.jetbrains.com/issue/KT-26366) UseExperimental with full qualified reference to marker annotation class is reported as error
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
- [`KT-27737`](https://youtrack.jetbrains.com/issue/KT-27737) CCE for delegated property of inline class type
- [`KT-27762`](https://youtrack.jetbrains.com/issue/KT-27762) The lexer crashes when a vertical tabulation is used
- [`KT-27774`](https://youtrack.jetbrains.com/issue/KT-27774) Update asm to 7.0 in Kotlin backend
- [`KT-27948`](https://youtrack.jetbrains.com/issue/KT-27948) Internal Error comparing UInt? to UInt
- [`KT-28054`](https://youtrack.jetbrains.com/issue/KT-28054) Inline class: "Cannot pop operand off an empty stack" for calling private secondary constructor from companion object
- [`KT-28185`](https://youtrack.jetbrains.com/issue/KT-28185) Incorrect behaviour of javaClass intrinsic for receivers of inline class type
- [`KT-28188`](https://youtrack.jetbrains.com/issue/KT-28188) CCE when bound callable reference with receiver of inline class type is passed to inline function
- [`KT-28385`](https://youtrack.jetbrains.com/issue/KT-28385) Rewrite at slice FUNCTION in MPP on "red" code
- [`KT-28405`](https://youtrack.jetbrains.com/issue/KT-28405) VE “Bad type on operand stack” at runtime on creating inline class with UIntArray inside
- [`KT-28585`](https://youtrack.jetbrains.com/issue/KT-28585) Inline classes not properly boxed when accessing a `var` (from enclosing scope) from lambda
- [`KT-6574`](https://youtrack.jetbrains.com/issue/KT-6574) Enum entry classes should be compiled to package private classes
- [`KT-20358`](https://youtrack.jetbrains.com/issue/KT-20358) Map.getOrDefault is available in Android, but it shouldn't
- [`KT-23543`](https://youtrack.jetbrains.com/issue/KT-23543) Back-end (JVM) Internal error: Couldn't inline method
- [`KT-24156`](https://youtrack.jetbrains.com/issue/KT-24156) For-loop optimization should not be applied in case of custom iterator
- [`KT-24780`](https://youtrack.jetbrains.com/issue/KT-24780) Recursive suspend local functions: "Expected an object reference, but found ."
- [`KT-26149`](https://youtrack.jetbrains.com/issue/KT-26149) No report about use contracts in not top-level functions or lambdas
- [`KT-26186`](https://youtrack.jetbrains.com/issue/KT-26186) External contract builder is not supported
- [`KT-28237`](https://youtrack.jetbrains.com/issue/KT-28237) CoroutineStackFrame uses slashes instead of dots in FQN

### IDE

#### New Features

- [`KT-25906`](https://youtrack.jetbrains.com/issue/KT-25906) Kotlin language injection doesn't evaluate constants in string templates
- [`KT-27461`](https://youtrack.jetbrains.com/issue/KT-27461) Provide live template to generate `main()` with no parameters
- [`KT-28371`](https://youtrack.jetbrains.com/issue/KT-28371) Automatically align ?: (elvis operator) after call on the new line

#### Performance Improvements

- [`KT-23738`](https://youtrack.jetbrains.com/issue/KT-23738) Provide stubs for annotation value argument list
- [`KT-25410`](https://youtrack.jetbrains.com/issue/KT-25410) Opening Settings freezes the UI for 23 seconds
- [`KT-28755`](https://youtrack.jetbrains.com/issue/KT-28755) Optimize searching constructor delegation calls
- [`KT-27832`](https://youtrack.jetbrains.com/issue/KT-27832) Improve performance of KotlinGradleProjectResolverExtension

#### Fixes

- [`KT-9840`](https://youtrack.jetbrains.com/issue/KT-9840) Right parenthesis doesn't appear after class name before the colon
- [`KT-17502`](https://youtrack.jetbrains.com/issue/KT-17502) Do not disable "Generate equals and hashCode" actions for data classes
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
- [`KT-27954`](https://youtrack.jetbrains.com/issue/KT-27954) Generate -> toString() using "Multiple templates with concatenation" should add spaces after commas
- [`KT-28348`](https://youtrack.jetbrains.com/issue/KT-28348) Don't log or wrap ProcessCanceledException
- [`KT-28401`](https://youtrack.jetbrains.com/issue/KT-28401) Show parameter info for lambdas during completion
- [`KT-28402`](https://youtrack.jetbrains.com/issue/KT-28402) Automatically indent || and && operators
- [`KT-28458`](https://youtrack.jetbrains.com/issue/KT-28458) New Project Wizard: move multiplatform projects to the new DSL
- [`KT-28513`](https://youtrack.jetbrains.com/issue/KT-28513) Bad Kotlin configuration when old syntax is used for configured Gradle project with >= 4.4 version
- [`KT-18807`](https://youtrack.jetbrains.com/issue/KT-18807) Complete Current Statement doesn't work in function call
- [`KT-26246`](https://youtrack.jetbrains.com/issue/KT-26246) JPS instead of Gradle run configuration is generated for a new MPP model
- [`KT-26247`](https://youtrack.jetbrains.com/issue/KT-26247) Unable to run/debug platform code in a new MPP model: incorrect run configuration generated
- [`KT-28289`](https://youtrack.jetbrains.com/issue/KT-28289) "Extend selection" doesn't select blank line after method in class
- [`KT-28556`](https://youtrack.jetbrains.com/issue/KT-28556) Wrong nullability for @JvmOverloads-generated method parameter in light classes

### IDE. Android

- [`KT-25450`](https://youtrack.jetbrains.com/issue/KT-25450) NoClassDefFoundError when trying to run a scratch file in Android Studio 3.1.3, Kotlin 1.2.51
- [`KT-26764`](https://youtrack.jetbrains.com/issue/KT-26764) `kotlin` content root isn't generated for Android module of a multiplatform project on Gradle import
- [`KT-23560`](https://youtrack.jetbrains.com/issue/KT-23560) Scratch: impossible to run scratch file from Android Studio

### IDE. Code Style, Formatting

- [`KT-24496`](https://youtrack.jetbrains.com/issue/KT-24496) IntelliJ IDEA: Formatting around addition / subtraction not correct for Kotlin
- [`KT-27847`](https://youtrack.jetbrains.com/issue/KT-27847) Destructured declaration continued on the next line is formatted with double indent
- [`KT-28227`](https://youtrack.jetbrains.com/issue/KT-28227) Formatter should not allow enum entries to be on one line with opening brace
- [`KT-28070`](https://youtrack.jetbrains.com/issue/KT-28070) Code style: "Align when multiline" option for "extends / implements list" changes formating of enum constants constructor parameters
- [`KT-28484`](https://youtrack.jetbrains.com/issue/KT-28484) Bad formatting for assignment when continuation for assignments is disabled

### IDE. Completion

- [`KT-18089`](https://youtrack.jetbrains.com/issue/KT-18089) Completion for nullable types without safe call rendered in gray color is barely visible
- [`KT-20706`](https://youtrack.jetbrains.com/issue/KT-20706) KDoc: Unneeded completion is invoked after typing a number/digit in a kdoc comment
- [`KT-22579`](https://youtrack.jetbrains.com/issue/KT-22579) Smart completion should present enum constants with higher rank
- [`KT-23834`](https://youtrack.jetbrains.com/issue/KT-23834) Code completion and auto import do not suggest extension that differs from member only in type parameter
- [`KT-25312`](https://youtrack.jetbrains.com/issue/KT-25312) Autocomplete for overridden members in `expected` class inserts extra `override` word
- [`KT-26632`](https://youtrack.jetbrains.com/issue/KT-26632) Completion: "data class" instead of "data"
- [`KT-18582`](https://youtrack.jetbrains.com/issue/KT-18582) MPP: show only actual elements in completion list if both actual and expect versions exist
- [`KT-27916`](https://youtrack.jetbrains.com/issue/KT-27916) Autocomplete val when auto-completing const

### IDE. Debugger

#### Fixes

- [`KT-13268`](https://youtrack.jetbrains.com/issue/KT-13268) Can't quick evaluate expression with Alt + Click without get operator
- [`KT-14075`](https://youtrack.jetbrains.com/issue/KT-14075) Debugger: Property syntax accesses private Java field rather than synthetic property accessor
- [`KT-22366`](https://youtrack.jetbrains.com/issue/KT-22366) Debugger doesn't stop on certain expressions
- [`KT-24343`](https://youtrack.jetbrains.com/issue/KT-24343) Debugger, Step Over: IllegalStateException on two consecutive breakpoints when first breakpoint is on an inline function call
- [`KT-24959`](https://youtrack.jetbrains.com/issue/KT-24959) Evaluating my breakpoint condition fails with exception
- [`KT-25667`](https://youtrack.jetbrains.com/issue/KT-25667) Exception in logs from WeakBytecodeDebugInfoStorage (NoStrataPositionManagerHelper)
- [`KT-26795`](https://youtrack.jetbrains.com/issue/KT-26795) Debugger crashes with NullPointerException when evaluating const value in companion object
- [`KT-26798`](https://youtrack.jetbrains.com/issue/KT-26798) Check that step into works with overrides in inline classes
- [`KT-27414`](https://youtrack.jetbrains.com/issue/KT-27414) Use "toString" to render values of inline classes in debugger
- [`KT-28342`](https://youtrack.jetbrains.com/issue/KT-28342) Can't evaluate the synthetic 'field' variable
- [`KT-28487`](https://youtrack.jetbrains.com/issue/KT-28487) ISE “resultValue is null: cannot find method generated_for_debugger_fun” on evaluating value of inline class
- [`KT-23585`](https://youtrack.jetbrains.com/issue/KT-23585) Evaluation of a static interface method call fails
- [`KT-28028`](https://youtrack.jetbrains.com/issue/KT-28028) IDEA is unable to find sources during debugging

### IDE. Decompiler

- [`KT-27284`](https://youtrack.jetbrains.com/issue/KT-27284) Disable highlighting in decompiled Kotlin bytecode
- [`KT-27460`](https://youtrack.jetbrains.com/issue/KT-27460) "Show Kotlin bytecode": "Internal error: null" for an inline extension property from a different file

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

### IDE. Folding

- [`KT-4551`](https://youtrack.jetbrains.com/issue/KT-4551) Fold/Unfold safe access operators

### IDE. Hints

- [`KT-13118`](https://youtrack.jetbrains.com/issue/KT-13118) Parameter info is not shown for Kotlin last-argument lambdas
- [`KT-27802`](https://youtrack.jetbrains.com/issue/KT-27802) The hint for the if-expression is duplicated inside each branch
- [`KT-26689`](https://youtrack.jetbrains.com/issue/KT-26689) Lambda return expression hint not shown when returning a lambda from inside a lambda

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
- [`KT-25171`](https://youtrack.jetbrains.com/issue/KT-25171) Inspection: Change indexed access operator on maps to `Map.getValue`
- [`KT-25620`](https://youtrack.jetbrains.com/issue/KT-25620) Inspection for functions returning Deferred
- [`KT-25718`](https://youtrack.jetbrains.com/issue/KT-25718) Add intention to convert SAM lambda to anonymous object
- [`KT-26236`](https://youtrack.jetbrains.com/issue/KT-26236) QuickFix for ASSIGN_OPERATOR_AMBIGUITY on mutable collection '+=', '-='
- [`KT-26511`](https://youtrack.jetbrains.com/issue/KT-26511) Inspection (without highlighting by default) for unlabeled return inside lambda
- [`KT-26653`](https://youtrack.jetbrains.com/issue/KT-26653) Intention to replace if-else with `x?.let { ... } ?: ...`
- [`KT-26724`](https://youtrack.jetbrains.com/issue/KT-26724) Inspection with a warning for implementation by delegation to a `var` property
- [`KT-27007`](https://youtrack.jetbrains.com/issue/KT-27007) Intention: add label to return if scope is visually ambiguous
- [`KT-27075`](https://youtrack.jetbrains.com/issue/KT-27075) Add a quick fix/intention to create `expect` member for an added `actual` declaration
- [`KT-27445`](https://youtrack.jetbrains.com/issue/KT-27445) Add quickfix for compiler warning "DEPRECATED_JAVA_ANNOTATION"
- [`KT-28118`](https://youtrack.jetbrains.com/issue/KT-28118) Remove empty parentheses for annotation entries
- [`KT-28631`](https://youtrack.jetbrains.com/issue/KT-28631) Suggest to remove single lambda argument if its name is equal to `it`
- [`KT-28696`](https://youtrack.jetbrains.com/issue/KT-28696) Inspection: detect potentially ambiguous usage of coroutineContext
- [`KT-4557`](https://youtrack.jetbrains.com/issue/KT-4557) Convert between named functions and their calls by conventions
- [`KT-24515`](https://youtrack.jetbrains.com/issue/KT-24515) Intention to add an exception under the cursor to @Throws annotations

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
- [`KT-20725`](https://youtrack.jetbrains.com/issue/KT-20725) Cannot persist excluded methods for inspection "Accessor call that can be replaced with property syntax"

### IDE. KDoc

- [`KT-24788`](https://youtrack.jetbrains.com/issue/KT-24788) Endless exceptions in offline inspections

### IDE. Multiplatform

- [`KT-26518`](https://youtrack.jetbrains.com/issue/KT-26518) `Create actual ...` quick fix doesn't add a primary constructor call for the actual secondary constructor
- [`KT-26957`](https://youtrack.jetbrains.com/issue/KT-26957) Merge expect gutter icon, when used for the same line
- [`KT-27335`](https://youtrack.jetbrains.com/issue/KT-27335) New multiplatform wizard: mobile library is generated with failed test
- [`KT-27595`](https://youtrack.jetbrains.com/issue/KT-27595) KNPE on attempt to generate `equals()`, `hashCode()`, `toString()` for `expect` class

### IDE. Navigation

- [`KT-27494`](https://youtrack.jetbrains.com/issue/KT-27494) Create tooling tests for new-multiplatform
- [`KT-28206`](https://youtrack.jetbrains.com/issue/KT-28206) Go to implementations on expect enum shows not only enum classes, but also all members
- [`KT-28398`](https://youtrack.jetbrains.com/issue/KT-28398) Broken navigation to actual declaration of `println()` in non-gradle project

### IDE. Project View

- [`KT-26210`](https://youtrack.jetbrains.com/issue/KT-26210) IOE “Cannot create file” on creating new file with existing filename by pasting a code in Project view
- [`KT-27903`](https://youtrack.jetbrains.com/issue/KT-27903) Can create file with empty name without any warning

### IDE. Refactorings

- [`KT-23603`](https://youtrack.jetbrains.com/issue/KT-23603) Add the support for find usages/refactoring of the buildSrc sources in gradle kotlin DSL build scripts
- [`KT-26696`](https://youtrack.jetbrains.com/issue/KT-26696) Copy, Move: "Destination directory" field does not allow to choose a path from non-JVM module
- [`KT-28408`](https://youtrack.jetbrains.com/issue/KT-28408) "Extract interface" action should not show private properties
- [`KT-28476`](https://youtrack.jetbrains.com/issue/KT-28476) Extract interface / super class on non-JVM class throws KNPE

### IDE. Scratch

- [`KT-25032`](https://youtrack.jetbrains.com/issue/KT-25032) Scratch: IDEA hangs/freezes on code that never returns (infinite loops)
- [`KT-26271`](https://youtrack.jetbrains.com/issue/KT-26271) Scratches for Kotlin  do not work when clicking "Run Scratch File" button
- [`KT-26332`](https://youtrack.jetbrains.com/issue/KT-26332) Fix classpath intention in Kotlin scratch file in Java only project doesn't do anything
- [`KT-27628`](https://youtrack.jetbrains.com/issue/KT-27628) Scratch blocks AWT Queue thread
- [`KT-23523`](https://youtrack.jetbrains.com/issue/KT-23523) Filter out fake gradle modules from checkbox in Scratch file panel

### IDE. Script

- [`KT-24465`](https://youtrack.jetbrains.com/issue/KT-24465) Provide a UI to manage script definitions
- [`KT-24466`](https://youtrack.jetbrains.com/issue/KT-24466) Add warning when there are multiple script definitions for one script
- [`KT-26331`](https://youtrack.jetbrains.com/issue/KT-26331) Please extract ScriptDefinitionContributor/KotlinScriptDefinition from kotlin-plugin.jar to separate jar
- [`KT-27669`](https://youtrack.jetbrains.com/issue/KT-27669) Consider moving expensive tasks out of the UI thread
- [`KT-27743`](https://youtrack.jetbrains.com/issue/KT-27743) Do not start multiple background threads loading dependencies for different scripts
- [`KT-27817`](https://youtrack.jetbrains.com/issue/KT-27817) Implement a lightweight EP in a separate public jar for supplying script definitions to IDEA
- [`KT-28046`](https://youtrack.jetbrains.com/issue/KT-28046) "Reload script dependencies on file change" option is missing after project restart
- [`KT-20762`](https://youtrack.jetbrains.com/issue/KT-20762) Kotlin script run configuration: support "Use classpath of module" attribute

### IDE. Tests Support

- [`KT-27977`](https://youtrack.jetbrains.com/issue/KT-27977) Missing 'run' gutter on a test method of an abstract class
- [`KT-28080`](https://youtrack.jetbrains.com/issue/KT-28080) Wrong run configuration created from context for test method in abstract class
- [`KT-25101`](https://youtrack.jetbrains.com/issue/KT-25101) Common tests cannot be run from IDE without manually editing run configuration

### JS. Tools

- [`KT-27249`](https://youtrack.jetbrains.com/issue/KT-27249) Support kotlin-frontend-plugin with the new kotlin-multiplatform plugin

### JavaScript

- [`KT-27611`](https://youtrack.jetbrains.com/issue/KT-27611) Calling a suspending function of a JS library causes "Uncaught ReferenceError: CoroutineImpl is not defined"
- [`KT-28215`](https://youtrack.jetbrains.com/issue/KT-28215) JS: inline suspend function not usable in non-inlined form
- [`KT-25003`](https://youtrack.jetbrains.com/issue/KT-25003) Invalid reference to COROUTINE_SUSPENDED is generated in JS
- [`KT-26706`](https://youtrack.jetbrains.com/issue/KT-26706) JS incorrect Long value in string templates
- [`KT-28207`](https://youtrack.jetbrains.com/issue/KT-28207) Finally block loops forever for specific code shape

### Libraries

- [`KT-18398`](https://youtrack.jetbrains.com/issue/KT-18398) Provide a way for libraries to avoid mixing Kotlin 1.0 and 1.1 dependencies in end user projects
- [`KT-20865`](https://youtrack.jetbrains.com/issue/KT-20865) Retrieving groups by name is not supported on Java 9 even with `kotlin-stdlib-jre8` in the classpath
- [`KT-25371`](https://youtrack.jetbrains.com/issue/KT-25371) Support unsigned integers in kotlinx-metadata-jvm
- [`KT-27251`](https://youtrack.jetbrains.com/issue/KT-27251) Do not use Stack in FileTreeWalk iterator implementation
- [`KT-27629`](https://youtrack.jetbrains.com/issue/KT-27629) kotlin.test BeforeTest/AfterTest annotation mapping for TestNG
- [`KT-27919`](https://youtrack.jetbrains.com/issue/KT-27919) Publish modularized artifacts under 'modular' classifier
- [`KT-28091`](https://youtrack.jetbrains.com/issue/KT-28091) Provide correct AbstractMutableCollections declarations in stdlib-common
- [`KT-28251`](https://youtrack.jetbrains.com/issue/KT-28251) Stdlib: Deprecated ReplaceWith `kotlin.math.log` replacement instead of `kotlin.math.ln`
- [`KT-28488`](https://youtrack.jetbrains.com/issue/KT-28488) Add clarification for COROUTINES_SUSPENDED documentation
- [`KT-28572`](https://youtrack.jetbrains.com/issue/KT-28572) readLine() stumbles at surrogate pairs
- [`KT-28088`](https://youtrack.jetbrains.com/issue/KT-28088) NoSuchMethodException: javafx.application.Platform.startup with new kotlinx-coroutines-javafx:1.0.1

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

### Tools. Compiler Plugins

- [`KT-24997`](https://youtrack.jetbrains.com/issue/KT-24997) Pass arguments to Kapt in human-readable format
- [`KT-24998`](https://youtrack.jetbrains.com/issue/KT-24998) Introduce separate command line tool specifically for Kapt in order to improve UX

### Tools. Gradle

#### New Features

- [`KT-26963`](https://youtrack.jetbrains.com/issue/KT-26963) Warn user that a custom platform-agnostic source set wouldn't be included into build unless it is required for other source sets
- [`KT-28155`](https://youtrack.jetbrains.com/issue/KT-28155) Add ability to run tasks in parallel within project

#### Performance Improvements

- [`KT-24530`](https://youtrack.jetbrains.com/issue/KT-24530) Enable compile avoidance for kaptKotlin tasks

#### Fixes

- [`KT-26065`](https://youtrack.jetbrains.com/issue/KT-26065) Kotlin Gradle plugin resolves dependencies at configuration time
- [`KT-26389`](https://youtrack.jetbrains.com/issue/KT-26389) Support Gradle Kotlin DSL in projects with the `kotlin-multiplatform` plugin
- [`KT-26663`](https://youtrack.jetbrains.com/issue/KT-26663) Gradle dependency DSL features missing for the new MPP dependencies
- [`KT-27682`](https://youtrack.jetbrains.com/issue/KT-27682) Kotlin MPP DSL: a target is missing the `attributes { ... }` function, only the `attributes` property is available.
- [`KT-27950`](https://youtrack.jetbrains.com/issue/KT-27950) Gradle 5.0-rc1: "Compilation with Kotlin compile daemon was not successful"
- [`KT-28363`](https://youtrack.jetbrains.com/issue/KT-28363) Enable resources processing for Kotlin/JS target in multiplatform projects
- [`KT-28520`](https://youtrack.jetbrains.com/issue/KT-28520) MPP plugin can't be applied altogether with the "maven-publish" plugin in a Gradle 5 build
- [`KT-28635`](https://youtrack.jetbrains.com/issue/KT-28635) fromPreset() in MPP Gradle plugin DSL is hard to use from Gradle Kotlin DSL scripts
- [`KT-26498`](https://youtrack.jetbrains.com/issue/KT-26498) NCDFE org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet after refreshing Gradle project with ktlint plugin
- [`KT-26688`](https://youtrack.jetbrains.com/issue/KT-26688) Build fails on compilation of a simple native module in a project with a new multiplatform model
- [`KT-26854`](https://youtrack.jetbrains.com/issue/KT-26854) MPP/Native: Can't use 'NativeOutputKind.FRAMEWORK' in build.gradle
- [`KT-26978`](https://youtrack.jetbrains.com/issue/KT-26978) Gradle verification fails on DiscoverScriptExtensionsTask
- [`KT-27231`](https://youtrack.jetbrains.com/issue/KT-27231) Exception thrown while executing model rule: PublishingPlugin.Rules#publishing(ExtensionContainer) on usage of 'maven-publish' plugin with Kotlin/Native generated project
- [`KT-27685`](https://youtrack.jetbrains.com/issue/KT-27685) In new MPP, expose a compilation's default source set via DSL

### Tools. J2K

- [`KT-26073`](https://youtrack.jetbrains.com/issue/KT-26073) Irrelevant "create extra commit with java->kt rename"

### Tools. JPS

- [`KT-26980`](https://youtrack.jetbrains.com/issue/KT-26980) JPS Native warning is duplicated for test source sets
- [`KT-27285`](https://youtrack.jetbrains.com/issue/KT-27285) MPP: invalid common -> platform dependency: JPS fails with Throwable "Cannot initialize Kotlin context: Cyclically dependent modules" at KotlinChunk.<init>()
- [`KT-27622`](https://youtrack.jetbrains.com/issue/KT-27622) JPS, JS: Resources marked as "kotlin-resource" are not copied to the out folder in a Kotlin-js project
- [`KT-28095`](https://youtrack.jetbrains.com/issue/KT-28095) JPS: support -Xcommon-sources for multiplatform projects (JS)
- [`KT-28316`](https://youtrack.jetbrains.com/issue/KT-28316) Report `Native is not yet supported in IDEA internal build system` on JPS build once per project/multiplatform module
- [`KT-28527`](https://youtrack.jetbrains.com/issue/KT-28527) JPS: Serialization plugin not loaded in ktor
- [`KT-26648`](https://youtrack.jetbrains.com/issue/KT-26648) Native modules of a multiplatform project wouldn't be built through JPS build

### Tools. Scripts

- [`KT-27382`](https://youtrack.jetbrains.com/issue/KT-27382) Embeddable version of scripting support (KEEP 75)
- [`KT-27497`](https://youtrack.jetbrains.com/issue/KT-27497) kotlin script -  No class roots are found in the JDK path

### Tools. kapt

- [`KT-24368`](https://youtrack.jetbrains.com/issue/KT-24368) Kapt: Do not include compile classpath entries in the annotation processing classpath
- [`KT-25756`](https://youtrack.jetbrains.com/issue/KT-25756) Investigate file descriptors leaks in kapt
- [`KT-26145`](https://youtrack.jetbrains.com/issue/KT-26145) Using `kapt` without the `kotlin-kapt` plugin should throw a build error
- [`KT-26725`](https://youtrack.jetbrains.com/issue/KT-26725) Kapt does not handle androidx.annotation.RecentlyNullable correctly
- [`KT-26817`](https://youtrack.jetbrains.com/issue/KT-26817) kapt 1.2.60+ ignores .java files that are symlinks
- [`KT-27188`](https://youtrack.jetbrains.com/issue/KT-27188) kapt Gradle plugin fails in Java 10+ ("Cannot find tools.jar")
- [`KT-27334`](https://youtrack.jetbrains.com/issue/KT-27334) [Kapt] Stub generator uses constant value in method annotation instead of constant name.
- [`KT-27404`](https://youtrack.jetbrains.com/issue/KT-27404) Kapt does not call annotation processors on custom (e.g., androidTest) source sets if all dependencies are inherited from the main kapt configuration
- [`KT-28025`](https://youtrack.jetbrains.com/issue/KT-28025) Detect memory leaks in annotation processors

## Previous releases
This release also includes the fixes and improvements from the [previous releases](https://github.com/JetBrains/kotlin/releases/tag/v1.3.11).
