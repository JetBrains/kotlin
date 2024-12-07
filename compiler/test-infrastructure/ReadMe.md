# Test infrastructure for Kotlin Compiler

## Introduction

This module includes core of test system for writing and executing compiler tests. Test system includes many tools for configuring test execution and allows a lot of customizations. This article will describe base principles of test system itself and details of different customization mechanisms.

## Test pipeline

### Module structure

Each test includes at least one module. Module is a base compilation entity which includes one or multiple files and some additional configuration information (such as target backend). Module can depend on other modules, but modules are processed in test in order of definition so don't refer to module in dependencies before its declaration to avoid errors

### Facades and kinds

[AbstractTestFacade](tests/org/jetbrains/kotlin/test/model/Facades.kt) is an object that takes some artifact for some module and produces another artifact. Artifact ([ResultingArtifact](tests/org/jetbrains/kotlin/test/model/ResultingArtifact.kt)) represents results of work of some facade. Each facade is parametrized by types of input artifact that it can accept and output artifact which it produces. For example there is a [ClassicFrontend2IrConverter](../tests-common-new/tests/org/jetbrains/kotlin/test/frontend/classic/ClassicFrontend2IrConverter.kt) facade which takes [artifact from FE 1.0 frontend](../tests-common-new/tests/org/jetbrains/kotlin/test/frontend/classic/ClassicFrontendOutputArtifact.kt) which contains `PSI` and `BindingContext` and transforms it to [backend input artifact](../tests-common-new/tests/org/jetbrains/kotlin/test/backend/ir/IrBackendInput.kt) which includes backend IR using `psi2ir`  

### Handlers

[AnalysisHandler](tests/org/jetbrains/kotlin/test/model/AnalysisHandler.kt) is a base class for entities which take some artifact and perform some checks over that artifact. Those checks can be anything from checking some invariant on artifact (e.g. that there is no unresolved types left after frontend is over) to dumping some information from artifact to file (e.g. dump of backend IR)

### Steps

[TestStep](tests/org/jetbrains/kotlin/test/TestStep.kt) is an abstraction of single step in pipeline of processing each module. There are two kinds of test steps:
1. `TestStep.FacadeStep`. This kind of step contains some facade and transforms input artifact to output artifact with it if type of input artifact matches with corresponding type of facade
2. `TestStep.HandlersStep`. This kind of step contains some handlers parameterized with single artifact kind and runs all its handlers if type of input artifact matches with type of handlers

### Steps pipeline

Each test defines multiple number of parametrized steps in specific order. [TestRunner](tests/org/jetbrains/kotlin/test/TestRunner.kt) (main entrypoint to test) takes configuration and module structure and performs next steps:
1. Parse module structure from testdata
2. For each module in test structure:
    1. Introduce `var artifact` which represents artifact produced by last facade
    2. Initialize it with input artifact that represents source code
    3. For each step from configuration:
        - If type of step input artifact didn't match to type of `artifact` go to next step
        - If step is
        - `FacadeStep`:
            - run facade and assign its result to `artifact`
            - if input type of facade differs from output type then register `artifact` in dependencies provider so other modules which depend on this module can use it
        - `HandlersStep`:
            - run all handlers from it on `artifact`
3. Run finalized methods of all handlers
4. Run some additional finalizers (description is below)

## Directives

Directives are main option for configuring test. With them you can configure files and modules in your test, compiler flags, enable and disable specific handlers etc. Directives are objects of specific class [Directive](tests/org/jetbrains/kotlin/test/directives/model/Directive.kt), and there are three different subclasses for three different types of directives (they all declared in [Directive.kt](tests/org/jetbrains/kotlin/test/directives/model/Directive.kt) file):
- `SimpleDirective` is a directive which can be only enabled or disabled
- `StringDirective` is a directive which may accept one or multiple string arguments
- `ValueDirective<T>` is a directive which may accept one or multiple arguments of type `T`

All directives should be declared in special containers which are inheritors of [SimpleDirectivesContainer](tests/org/jetbrains/kotlin/test/directives/model/DirectivesContainer.kt). There are multiple utility functions in [SimpleDirectivesContainer](tests/org/jetbrains/kotlin/test/directives/model/DirectivesContainer.kt) which **should** be used for declaring directives:
- function `directive()` declares `SimpleDirective`
- function `stringDirective()` declares `StringDirective`
- function `valueDirective<T>()` takes parser of type `(String) -> T?` and declares `ValueDirective<T>`. Parser function is needed to transform arguments from testdata to real values of type `T`
- function `enumDirective<T>()` is needed to create `ValueDirective<T>` of enum type `T`. It doesn't require `parser` function and parse enum values by their names. Note that you can pass `additionalParser: (String) -> T?` as a fallback parsing option.

All these functions also take the following arguments:
- `description: String`: required parameter which should include description of this directive
- `applicability: DirectiveApplicability`: with this optional argument you can configure where this directive can be applicable if you test contains multiple files or modules. By default all directives have `Global` applicability which means that directive can be declared at global or module level, but not in test files (read about files and modules in [Module structure](#module-structure))

Name of directive will be same as name of directive property created by one of those functions. Note that all of the `*directive()` functions provide a property delegate, so you should create directives using `by directive()`, not `= directive()`. 

As an example of directive container you can check [directives for configuring language settings](tests/org/jetbrains/kotlin/test/directives/LanguageSettingsDirectives.kt).

In testdata file you should declare directives using following syntax:
- `// DIRECTIVE` for simple directives
- `// DIRECTIVE: arg[, arg2, arg3]` for directives with parameters

### Module structure directives

Test framework supports tests which contain different source files or modules in single testdata file. There are two directives which are needed to split a testdata file:
- Directive `// FILE: fileName.kt` says that all content until next module structure directive belongs to file `fileName.kt`
- Directive `// MODULE: moduleName` says that all files until next `MODULE` directive belongs to module `moduleName`

If there are no `MODULE` directives in testdata file, then all files belong to a default module with the name `main`.

If there are no `FILE` directives in module, then all content of module belongs to a default file with the name `main.kt`.

#### Module dependencies

Each module can declare that it depends on some other module with following syntax:
```
// MODULE: name[(dep1, dep2)[(friend 1, friend2)][(refined dep 1, refiend dep 2)]]
```
- if module has no friend modules, you can write just `// MODULE: name(dep1, dep2)`
- if module has no dependencies at all, you can write only module name: `// MODULE: name`
- if module has no dependencies but has friends, then you should declare empty parentheses of dependencies: `// MODULE: name()(friend1, friend2)`
- if module does not have normal dependencies but has refined ones, then you should declare empty parentheses for first two kinds: `// MODULE: name()()(refined dep 1, refiend dep 2)`

# Implementation details

## Test services

Different parts of test (like facades and handlers) may use some additional components which contain some logic which can be shared between different those parts. For such components there is a special class [TestServices](tests/org/jetbrains/kotlin/test/services/TestServices.kt) which is a strongly typed container of test services (inheritors of interface `TestService`). All test services are initialized before the test is started and persist only until the test is finished, which means that it's safe to store some caches for specific test in services.

To declare your own service you need to do three things:
1. Declare class of service with one constructor which takes `TestServices` as parameter and inherit it from `TestService` interface
```kotlin
class MySuperService(val testServices: TestServices) : TestService {
    ...
}
```
2. Add typed accessor to this service from `TestServices`
```kotlin
val TestServices.mySuperService: MySuperService by TestServices.testServiceAccessor()
```
3. Register service inside test. There are two ways to register service:
- A lot of different test entities (like facades or handlers) are marked with `ServicesAndDirectivesContainer` interface, which has `additionalService` field with list of services this entity uses. During test configuration, infrastructure collects all those additional services, creates instances of them and registers inside `TestServices`
```kotlin
class MyHandler : AnalysisHandler<MyArtifact>() {
    override val additionalServices: List<ServiceRegistrationData> = listOf(service(::MySuperService))
}
```
- You also can manually register service using test configuration builder DSL (which will be fully described below)
```kotlin
override fun TestConfigurationBuilder.configure() {
    useAdditionalService(::MySuperService)
    ...
}
```

### Existing services

Here are some existing services which are useful in a wide range of different test cases:
- [BackendKindExtractor](tests/org/jetbrains/kotlin/test/services/BackendKindExtractor.kt) transforms `TargetBackend` to `BackendKind`
- [SourceFileProvider](tests/org/jetbrains/kotlin/test/services/SourceFileProvider.kt) retrieves content of test files from test modules
- [KotlinTestInfo](tests/org/jetbrains/kotlin/test/services/KotlinTestInfo.kt) contains info about test (test name, test class, etc)
- [DependencyProvider](tests/org/jetbrains/kotlin/test/services/DependencyProvider.kt) caches and provides artifacts of modules analyzed by facade steps
- [Assertions](tests/org/jetbrains/kotlin/test/services/Assertions.kt) contains utility assertions methods. This service is needed to abstract assertions infrastructure from any existing test framework (most commonly used assertions implementation is [JUnit5Assertions](../tests-common-new/tests/org/jetbrains/kotlin/test/services/JUnit5Assertions.kt))
- [CompilerConfigurationProvider](../tests-common-new/tests/org/jetbrains/kotlin/test/services/CompilerConfigurationProvider.kt) provider of compiler configuration for different modules (additional info below)
- [TemporaryDirectoryManager](tests/org/jetbrains/kotlin/test/services/TemporaryDirectoryManager.kt) can create temporary directories for test purposes (e.g. directory to write generated .class files)

There are many other services, you can find them by looking at inheritors of `TestService`.

#### Compiler configuration provider

[CompilerConfiguration](../config/src/org/jetbrains/kotlin/config/CompilerConfiguration.java) is main class which configures how specific module will be analyzed or compiled and for its setup there is a special service named [CompilerConfigurationProvider](../tests-common-new/tests/org/jetbrains/kotlin/test/services/CompilerConfigurationProvider.kt). It creates `CompilerConfiguration` which is based on list of [EnvironmentConfigurators](tests/org/jetbrains/kotlin/test/services/EnvironmentConfigurator.kt) which can be registered in test. So if you want to customize compiler configuration you need to modify existing configurator (e.g. [JvmEnvironmentConfigurator](../tests-common-new/tests/org/jetbrains/kotlin/test/services/configuration/JvmEnvironmentConfigurator.kt)) or write your own. Main method of `EnvironmentConfigurator` is `configureCompilerConfiguration` which takes compiler configuration and test module, so you can configure configuration (sorry for tautology) using directives which are applied to specific module.

There are also two additional methods which can be used to provide some simple mapping:
1. from some value directive to configuration key of same type
2. from some directive to analysis flag

Here is short example of declaring your own environment configurator:

```kotlin
class MySuperEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(MyDirectives)

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (MyDirectives.MU_DIRECTIVE_1 in module.directives) {
            configuration.put(SOME_KEY, 1)
            configuration.put(SOME_OTHER_KEY, 2)
        }
    }

    override fun DirectiveToConfigurationKeyExtractor.provideConfigurationKeys() {
        register(MyDirectives.MY_ENUM, JVMConfigurationKeys.MY_ENUM)
    }

    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion
    ): Map<AnalysisFlag<*>, Any?> {
        return mapOf(AnalysisFlags.someFlag to true)
    }
}
```

To enable your configuration in test you should use `useConfigurators` method of test configuration DSL

```kotlin
override fun TestConfigurationBuilder.configure() {
    useConfigurators(::MySuperEnvironmentConfigurator)
    ...
}
```

#### Source file providers

Sometimes you may want to add some existing file (e.g. pack of helper functions) to multiple test cases with some directive. For that you may use [AdditionalSourceProvider](tests/org/jetbrains/kotlin/test/services/AdditionalSourceProvider.kt). This service takes test module and returns list of additional test files, which can be created from regular `File` using `toTestFile` method. If you want to make new `TestFile` manually please ensure that it has flag `isAdditional` set to `true`. This flag removes additional files processing from some handlers.

# Writing handlers

Basic [AnalysisHandler](tests/org/jetbrains/kotlin/test/model/AnalysisHandler.kt) has following declaration:

```kotlin
abstract class AnalysisHandler<A : ResultingArtifact<A>>(
    val testServices: TestServices,
    val failureDisablesNextSteps: Boolean,
    val doNotRunIfThereWerePreviousFailures: Boolean
) : ServicesAndDirectivesContainer {
    abstract val artifactKind: TestArtifactKind<A>

    abstract fun processModule(module: TestModule, info: A)

    abstract fun processAfterAllModules(someAssertionWasFailed: Boolean)
}
```

`processModule` is called for each module with artifact of `artifactKind` if such artifact was produced by facade step. `processAfterAllModules` is called after all modules are analyzed so you can collect some information for each module, combine it to one piece in `processAfterAllModules` and assert something on it.

Boolean flags in the constructor define interaction of specific handler with other handlers and steps:
- if `failureDisablesNextSteps` is set to `true`, then failure in `processModule` will disable following steps for this module
- if `doNotRunIfThereWerePreviousFailures` is set to `true`, then this particular handler will be skipped if there were exceptions from handlers which were called before

Please note that handler's constructor should have shape `(TestServices) -> MyHandler`, so you need to specify flags from `AnalysisHandler` constructor manually.

### Handler tools

There are three general types of handlers:
1. Handler which checks some invariant on artifact and raises exception if that invariant is broken
2. Handlers which want to dump some information for each module and compare it with existing expected dump (usually saved in file). Example of handler: [IrTextDumpHandler](../tests-common-new/tests/org/jetbrains/kotlin/test/backend/handlers/IrTextDumpHandler.kt)
3. Handlers which want to render some information right in original testdata file, like [ClassicDiagnosticsHandler](../tests-common-new/tests/org/jetbrains/kotlin/test/frontend/classic/handlers/ClassicDiagnosticsHandler.kt) which renders diagnostics reported by FE 1.0 in testdata in `<!DIAGNOSCIT_NAME!>someExpression<!>` format

In test infrastructure there are some tools which can be useful for handlers of type 2. and 3.

#### MultiModuleInfoDumper

[MultiModuleInfoDumper](../tests-common-new/tests/org/jetbrains/kotlin/test/utils/MultiModuleInfoDumper.kt) is simple tool which can create separate string builders for different modules and produce resulting string from it. Here is simple example of `MultiModuleInfoDumper` usage:

```kotlin
class MySuperHandler(testServices: TestServices) : AnalysisHandler<ClassicFrontendOutputArtifact>(testServices, false, false) {
    override val artifactKind: TestArtifactKind<ClassicFrontendOutputArtifact>
        get() = FrontendKinds.ClassicFrontend
    
    private val dumper = MultiModuleInfoDumperImpl()
    
    override fun processModule(module: TestModule, info: ClassicFrontendOutputArtifact) {
        val builder = dumper.builderForModule(module)
        builder.appendLine("---- This is dump from module ${module.name} ----")
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val expectedFile = testServices.moduleStructure.originalTestDataFiles.first().withExtension(".myDump.txt")
        assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump())
    }
}
```

For test with two modules `A` and `B` this handler will generate dump

```
Module: A
---- This is dump from module A ----

Module: B
---- This is dump from module B ----
```

Header of dump of specific module can be configured in constructor of `MultiModuleInfoDumper`

### Meta infos

Handlers of type 3. (which want to render something inside original test file) can not use simple file dumps because:
1. There can be multiple handlers which want to report something and we need to combine their dumps in single file
2. Before running test someone needs to clean all dumps from original testdata, otherwise testfile can be incorrect and test fails

To handle these two problems there is additional infrastructure which uses `CodeMetaInfo` and `GlobalMetadataInfoHandler`.

#### CodeMetaInfo

[CodeMetaInfo](../test-infrastructure-utils/tests/org/jetbrains/kotlin/codeMetaInfo/model/CodeMetaInfo.kt) is a base abstraction for any kind of information you want to render. Basically it contains start and end offsets in original file, `tag` which is main name of meta info, attributes (additional arguments of meta info) and `renderConfiguration`, which describes how this meta info should be rendered in code. Default syntax for meta info is `<!TAG[attr1, attr2]!>text of original code<!>` (`[attr]` part will be omitted if attributes are empty).

#### GlobalMetadataInfoHandler

[GlobalMetadataInfoHandler](../test-infrastructure/tests/org/jetbrains/kotlin/test/services/GlobalMetadataInfoHandler.kt) is a service which is used for working with meta infos from handlers. It serves two purposes:
1. Parsing meta infos in original testdata, stripping them from it before passing code to steps and providing info about existing meta infos to handlers (`getExistingMetaInfosForFile` method)
2. Collecting meta infos from all handlers, rendering all of them to original test file and comparing it with expected test file on disk

So if your handler wants to report meta infos, all it needs is to create meta info instances and pass them to `GlobalMetadataInfoHandler` using `addMetadataInfosForFile` method (`GlobalMetadataInfoHandler` is a test service and is accessible via `testServices.globalMetadataInfoHandler`). Also you need to enable `GlobalMetadataInfoHandler` in test using `enableMetaInfoHandler()` method in test configuration DSL.

# Writing your own test. Test configuration DSL

One of main ideas of this test infrastructure is provide ability to define tests in declarative way: describe only _what_ will happen in test (what will be configured, which facades and handlers will be run), not _how_ it will be. To achieve this, a special DSL was developed, which is used to describe a test. Whole configuration of test is defined in class [TestConfiguration](tests/org/jetbrains/kotlin/test/TestConfiguration.kt), and there is also a [TestConfigurationBuilder](../tests-common-new/tests/org/jetbrains/kotlin/test/builders/TestConfigurationBuilder.kt) class which defines DSL for configuring all parts of test configuration. Here I highlight only most important parts of DSL, you can read the full specification in code of `TestConfigurationBuilder`.

- `defaultDirectives` allows defining directives which will be enabled in tests by default. It supports all kinds of directives:
```kotlin
defaultDirectives {
    +SOME_SIMPLE_DIRECTIVE // enable SOME_SIMPLE_DIRECTIVE
    -ANOTHER_SIMPLE_DIRECTIVE // disable directive if it was enabled before by other `defaultDirectives block
    
    STRING_DIRECTIVE with listOf("foo", "bar") // Add STRING_DIRECTIVE with values "foo" and "bar"
    VALUE_DIRECTIVE with Enum.SomeValue // Add VALUE_DIRECTIVE with value  Enum.SomeValue
}
```
- `useSomething` for registering different kinds of test services
    - `useConfigurators` for `EnvironmentConfigurator`
    - `useAdditionalSourceProviders` for `AdditionalSourceProvider`
    - `useAdditionalService` for some custom service
- `facadeStep` to register new facade step
- `handlersStep` and `namedHandlersStep` to register new handlers step
    - these methods take lambda in which you can call `useHandlers` method to register specific handlers
    - note that all steps will be executed in order of definition
    - if you create `namedHandlerStep` you can add additional handlers to it using `configureNamedHandlersStep` later; it can be useful in case you declare steps in one method (to share this pipeline between different tests) and configure them in specific test runners
- `enableMetaInfoHandler` for enabling `GlobalMetadataInfoHandler`
- `forTestsMatching` and `forTestsNotMatching` are methods which can be used to apply some configuration only if path to testdata file matches/not matches with regular expression which was passed to this method. These methods take lambda with `TestConfigurationBuilder` receiver so all methods listed above are accessible in it
    - these methods have two overloads: first takes `Regex` and second takes `String`, which is converted to `Regex` by simply replacing all `*` symbols with `.*` pattern

Almost all methods of DSL take `Constructor<SomeService>` as parameter, and `Constructor<T>` is just typealias to `(TestService) -> T`. If your service has constructor of such shape you can just pass callable reference to it (`useHandlers(::MyHandler)`). If your service is parametrized and has some additional parameter you can pass this parameter to service using function `bind`:
```kotlin
class MyHandler(testServices: TestServices, val someFlag: Boolean) : AnalysisHandler...

...
useHandlers(::MyHandler.bind(true))
...

// declaration of bind:
fun <T, R> ((TestServices, T) -> R).bind(value: T): Constructor<R> {
    return { this.invoke(it, value) }
}
```
    
## AbstractKotlinCompilerTest

[AbstractKotlinCompilerTest](../tests-common-new/tests/org/jetbrains/kotlin/test/runners/AbstractKotlinCompilerTest.kt) is a base class for all Kotlin compiler tests. It defines some default configuration and provides simple abstract method to implement `abstract fun TestConfigurationBuilder.configuration()` in inheritors. Whole test configuration should be described in override of this method

```kotlin
abstract class MyAbstractTestRunner : AbstractKotlinCompilerTest() {
    override fun TestConfigurationBuilder.configuration() {
        // describe configuration
    }
}
```

If you have hierarchy of test runners then there is no simple way to override `configuration()` method again and call `super.configuration` (because Kotlin unfortunately [cannot](https://youtrack.jetbrains.com/issue/KT-11488) call to super member with extension receiver), so for such cases you should use the following workaround:

```kotlin
abstract class MyAnotherAbstractTestRunner : MyAbstractTestRunner() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            // describe configuration
        }
    }
}
```

# Debugging tests (JS and WASM only):

Kotlin/JS and Kotlin/WASM tests support the following system properties to make debugging them more convenient:
- `kotlin.js.debugMode` for setting a debug mode for Kotlin/JS tests
- `kotlin.wasm.debugMode` for setting a debug mode for Kotlin/WASM tests
- `org.jetbrains.kotlin.compiler.ir.dump.strategy` for the IR dump strategy to use. Set it to `"KotlinLike"`
   if you want the IR dump to be more human-readable.

Note that to pass a system property from gradle task invocation, its name should be prefixed with `fd.`.
For example, to debug Kotlin/JS tests, run:

```
./gradlew :js:js.tests:jsIrTest -Pfd.kotlin.js.debugMode=2
```

To debug WASM tests, run:
```
./gradlew :wasm:wasm.tests:test -Pfd.kotlin.wasm.debugMode=2
```

Values of the debug mode: `0` (or `false`), `1` (or `true`), `2`.

Debug mode `2` will ensure that IR is dumped to a file after each lowering phase.
The IR dumps will appear next to the generated `.js` or `.wat` file.

# Massive testdata updating

There is a handler [UpdateTestDataHandler](../tests-common-new/tests/org/jetbrains/kotlin/test/backend/handlers/UpdateTestDataHandler.kt),
which can be used to update all testData. It is disabled by default. It can be enabled by either changing code,
or by passing system property `kotlin.test.update.test.data`.

For example, to update all IR text test data by the output of the JVM backend, you can use:
```bash
./gradlew -Pkotlin.test.update.test.data=true :compiler:fir:fir2ir:test --tests "org.jetbrains.kotlin.test.runners.ir.FirPsiJvmIrTextTestGenerated" --continue
```

# Code style

Please keep your abstract test runners as simple as possible. Ideally each abstract test runner should contain **only** test configuration with DSL and nothing else. All services implementations should be declared in separate files. 

Also please keep structure of packages. Abstract test runners are located in package `runners`, services in `services`, handlers in `handlers` etc.
