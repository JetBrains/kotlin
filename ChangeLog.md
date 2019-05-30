# CHANGELOG

## 1.3.40

### Android

#### Fixes

- [`KT-12402`](https://youtrack.jetbrains.com/issue/KT-12402) Android DataBinding work correctly but the IDE show it as error
- [`KT-31432`](https://youtrack.jetbrains.com/issue/KT-31432) Remove obsolete code introduced in KT-12402

### Compiler

#### New Features

- [`KT-29915`](https://youtrack.jetbrains.com/issue/KT-29915) Implement `typeOf` on JVM
- [`KT-30467`](https://youtrack.jetbrains.com/issue/KT-30467) Provide a way to to save compiled script(s) as a jar

#### Performance Improvements

- [`KT-17755`](https://youtrack.jetbrains.com/issue/KT-17755) Optimize trimIndent and trimMargin on constant strings
- [`KT-30603`](https://youtrack.jetbrains.com/issue/KT-30603) Compiler performance issue: VariableLivenessKt.useVar performance

#### Fixes

- [`KT-19227`](https://youtrack.jetbrains.com/issue/KT-19227) Load built-ins from dependencies by default in the compiler, support erroneous "fallback" built-ins
- [`KT-23426`](https://youtrack.jetbrains.com/issue/KT-23426) Actual typealias to Java enum does not match expected enum because of modality
- [`KT-23854`](https://youtrack.jetbrains.com/issue/KT-23854) Inference for common type of two captured types
- [`KT-25105`](https://youtrack.jetbrains.com/issue/KT-25105) False-positive warning "Remove final upper bound" on generic override
- [`KT-25302`](https://youtrack.jetbrains.com/issue/KT-25302) New inference: "Type mismatch" between star projection and `Any?` type argument in specific case
- [`KT-25433`](https://youtrack.jetbrains.com/issue/KT-25433) Wrong order of fixing type variables for callable references
- [`KT-26386`](https://youtrack.jetbrains.com/issue/KT-26386) Front-end recursion problem while analyzing contract function with call expression of self in implies
- [`KT-26412`](https://youtrack.jetbrains.com/issue/KT-26412) Wrong LVT generated if decomposed parameter of suspend lambda is not the first parameter.
- [`KT-27097`](https://youtrack.jetbrains.com/issue/KT-27097) JvmMultifileClass + JvmName causes NoSuchMethodError on sealed class hierarchy for top-level members
- [`KT-28534`](https://youtrack.jetbrains.com/issue/KT-28534) Local variable entries are missing in LVT for suspend lambda parameters
- [`KT-28535`](https://youtrack.jetbrains.com/issue/KT-28535) Rename `result` to `$result` in coroutines' LVT
- [`KT-29184`](https://youtrack.jetbrains.com/issue/KT-29184) Implement inference for coroutines according to the @BuilderInference contract in NI
- [`KT-29772`](https://youtrack.jetbrains.com/issue/KT-29772) Contracts don't work if `contract` function is fully qualified (FQN)
- [`KT-29790`](https://youtrack.jetbrains.com/issue/KT-29790) Incorrect version requirement in metadata of anonymous class for suspend lambda
- [`KT-29948`](https://youtrack.jetbrains.com/issue/KT-29948) NI: incorrect DSLMarker behaviour with generic star projection
- [`KT-30021`](https://youtrack.jetbrains.com/issue/KT-30021) +NewInference on Kotlin Native :: java.lang.StackOverflowError
- [`KT-30242`](https://youtrack.jetbrains.com/issue/KT-30242) Statements are not coerced to Unit in last expressions of lambda
- [`KT-30243`](https://youtrack.jetbrains.com/issue/KT-30243) Include FIR modules into compiler
- [`KT-30250`](https://youtrack.jetbrains.com/issue/KT-30250) Rewrite at slice exception for callable reference argument inside delegated expression
- [`KT-30292`](https://youtrack.jetbrains.com/issue/KT-30292) Reference to function is unresolved when LHS is a star-projected type
- [`KT-30293`](https://youtrack.jetbrains.com/issue/KT-30293) Wrong intersection type for common supertype from String and integer type
- [`KT-30370`](https://youtrack.jetbrains.com/issue/KT-30370) Call is completed too early when there is "Nothing" constraint
- [`KT-30405`](https://youtrack.jetbrains.com/issue/KT-30405) Support expected type from cast in new inference
- [`KT-30406`](https://youtrack.jetbrains.com/issue/KT-30406) Fix testIfOrWhenSpecialCall test for new inference
- [`KT-30590`](https://youtrack.jetbrains.com/issue/KT-30590) Report diagnostic about not enough information for inference in NI
- [`KT-30620`](https://youtrack.jetbrains.com/issue/KT-30620) Exception from the compiler when coroutine-inference is involved even with the explicitly specified types
- [`KT-30656`](https://youtrack.jetbrains.com/issue/KT-30656) Exception is occurred when functions with implicit return-stub types are involved in builder-inference
- [`KT-30658`](https://youtrack.jetbrains.com/issue/KT-30658) Exception from the compiler when getting callable reference to a suspend function
- [`KT-30661`](https://youtrack.jetbrains.com/issue/KT-30661) Disable SAM conversions to Kotlin functions in new-inference by default
- [`KT-30676`](https://youtrack.jetbrains.com/issue/KT-30676) Overload resolution ambiguity when there is a callable reference argument and candidates with different functional return types
- [`KT-30694`](https://youtrack.jetbrains.com/issue/KT-30694) No debug metadata is generated for suspend lambdas which capture crossinline
- [`KT-30724`](https://youtrack.jetbrains.com/issue/KT-30724) False positive error about missing equals when one of the operands is incorrectly inferred to Nothing
- [`KT-30734`](https://youtrack.jetbrains.com/issue/KT-30734) No smartcast inside lambda literal in then/else "if" branch
- [`KT-30737`](https://youtrack.jetbrains.com/issue/KT-30737) Try analysing callable reference preemptively
- [`KT-30780`](https://youtrack.jetbrains.com/issue/KT-30780) Compiler crashes on 'private inline' function accessing private constant in 'inline class' (regression)
- [`KT-30808`](https://youtrack.jetbrains.com/issue/KT-30808) NI: False negative SPREAD_OF_NULLABLE with USELESS_ELVIS_RIGHT_IS_NULL
- [`KT-30816`](https://youtrack.jetbrains.com/issue/KT-30816) BasicJvmScriptEvaluator passes constructor parameters in incorrect order
- [`KT-30826`](https://youtrack.jetbrains.com/issue/KT-30826) There isn't report about unsafe call in the new inference (by invalidating smartcast), NPE
- [`KT-30843`](https://youtrack.jetbrains.com/issue/KT-30843) Duplicate JVM class name for expect/actual classes in JvmMultifileClass-annotated file
- [`KT-30853`](https://youtrack.jetbrains.com/issue/KT-30853) Compiler crashes with NewInference and Kotlinx.Coroutines Flow
- [`KT-30927`](https://youtrack.jetbrains.com/issue/KT-30927) Data flow info isn't used for 'this' which is returned from lambda using labeled return
- [`KT-31081`](https://youtrack.jetbrains.com/issue/KT-31081) Implement ArgumentMatch abstraction in new inference
- [`KT-31113`](https://youtrack.jetbrains.com/issue/KT-31113) Fix failing tests from SlicerTestGenerated
- [`KT-31199`](https://youtrack.jetbrains.com/issue/KT-31199) Unresolved callable references with typealias
- [`KT-31339`](https://youtrack.jetbrains.com/issue/KT-31339) Inliner does not remove redundant continuation classes, leading to CNFE in JMH bytecode processing
- [`KT-31346`](https://youtrack.jetbrains.com/issue/KT-31346) Fix diagnostic DSL_SCOPE_VIOLATION for new inference
- [`KT-31356`](https://youtrack.jetbrains.com/issue/KT-31356) False-positive error about violating dsl scope for new-inference
- [`KT-31360`](https://youtrack.jetbrains.com/issue/KT-31360) NI: inconsistently prohibits member usage without explicit receiver specification with star projection and DSL marker
- [`KT-18563`](https://youtrack.jetbrains.com/issue/KT-18563) Do not generate inline reified functions as private in bytecode
- [`KT-20849`](https://youtrack.jetbrains.com/issue/KT-20849) Inference results in Nothing type argument in case of passing 'out T' to 'in T1'
- [`KT-25290`](https://youtrack.jetbrains.com/issue/KT-25290) New inference: KNPE at ResolutionPartsKt.getExpectedTypeWithSAMConversion() on out projection of Java class
- [`KT-26418`](https://youtrack.jetbrains.com/issue/KT-26418) Back-end (JVM) Internal error when compiling decorated suspend inline functions
- [`KT-26925`](https://youtrack.jetbrains.com/issue/KT-26925) Decorated suspend inline function continuation resumes in wrong spot
- [`KT-28999`](https://youtrack.jetbrains.com/issue/KT-28999) Prohibit type parameters for anonymous objects
- [`KT-29307`](https://youtrack.jetbrains.com/issue/KT-29307) New inference: false negative CONSTANT_EXPECTED_TYPE_MISMATCH with a Map
- [`KT-29475`](https://youtrack.jetbrains.com/issue/KT-29475) IllegalArgumentException at getAbstractTypeFromDescriptor with deeply nested expression inside function named with a right parenthesis
- [`KT-29996`](https://youtrack.jetbrains.com/issue/KT-29996) Properly report  errors on attempt to inline bytecode from class files compiled to 1.8 to one compiling to 1.6
- [`KT-30289`](https://youtrack.jetbrains.com/issue/KT-30289) Don't generate annotations on synthetic methods for methods with default values for parameters
- [`KT-30410`](https://youtrack.jetbrains.com/issue/KT-30410) [NI] Front-end recursion problem while analyzing contract function with call expression of self in implies
- [`KT-30411`](https://youtrack.jetbrains.com/issue/KT-30411) Fold recursive types to star-projected ones when inferring type variables
- [`KT-30706`](https://youtrack.jetbrains.com/issue/KT-30706) Passing noinline lambda as (cross)inline parameter result in wrong state-machine
- [`KT-30707`](https://youtrack.jetbrains.com/issue/KT-30707) Java interop of coroutines inside inline functions is broken
- [`KT-30983`](https://youtrack.jetbrains.com/issue/KT-30983) ClassCastException: DeserializedTypeAliasDescriptor cannot be cast to PackageViewDescriptor on star-import of expect enum class actualized with typealias
- [`KT-31242`](https://youtrack.jetbrains.com/issue/KT-31242) "Can't find enclosing method" proguard compilation exception with inline and crossinline
- [`KT-31347`](https://youtrack.jetbrains.com/issue/KT-31347) "IndexOutOfBoundsException: Insufficient maximum stack size" with crossinline and suspend
- [`KT-31354`](https://youtrack.jetbrains.com/issue/KT-31354) Suspend inline functions with crossinline parameters are inaccessible from java
- [`KT-31367`](https://youtrack.jetbrains.com/issue/KT-31367) IllegalStateException: Concrete fake override public open fun (...)  defined in TheIssue[PropertyGetterDescriptorImpl@1a03c376] should have exactly one concrete super-declaration: []
- [`KT-31461`](https://youtrack.jetbrains.com/issue/KT-31461) NI: NONE_APPLICABLE instead of TYPE_MISMATCH when invoking convention plus operator
- [`KT-31503`](https://youtrack.jetbrains.com/issue/KT-31503) Type mismatch with recursive types and SAM conversions
- [`KT-31507`](https://youtrack.jetbrains.com/issue/KT-31507) Enable new type inference algorithm for IDE analysis
- [`KT-31514`](https://youtrack.jetbrains.com/issue/KT-31514) New inference generates multiple errors on generic inline expression with elvis operator
- [`KT-31520`](https://youtrack.jetbrains.com/issue/KT-31520) False positive "not enough information" for constraint with star projection and covariant type
- [`KT-31606`](https://youtrack.jetbrains.com/issue/KT-31606) Rewrite at slice on using callable reference with array access operator
- [`KT-31620`](https://youtrack.jetbrains.com/issue/KT-31620) False-positive "not enough information" for coroutine-inference when target method is assigned to a variable
- [`KT-31624`](https://youtrack.jetbrains.com/issue/KT-31624) Type from declared upper bound in Java is considered more specific than Nothing producing type mismatch later

### IDE

#### New Features

- [`KT-11242`](https://youtrack.jetbrains.com/issue/KT-11242) Action to copy project diagnostic information to clipboard
- [`KT-24292`](https://youtrack.jetbrains.com/issue/KT-24292) Support external nullability annotations
- [`KT-30453`](https://youtrack.jetbrains.com/issue/KT-30453) Add plugin option (registry?) to enable new inference only in IDE

#### Performance Improvements

- [`KT-13841`](https://youtrack.jetbrains.com/issue/KT-13841) Classes and functions should be lazy-parseable
- [`KT-27106`](https://youtrack.jetbrains.com/issue/KT-27106) Performance issue with optimize imports
- [`KT-30442`](https://youtrack.jetbrains.com/issue/KT-30442) Several second lag on project open in KotlinNonJvmSourceRootConverterProvider
- [`KT-30644`](https://youtrack.jetbrains.com/issue/KT-30644) ConfigureKotlinInProjectUtilsKt freezes UI

#### Fixes

- [`KT-7380`](https://youtrack.jetbrains.com/issue/KT-7380) Imports insertion on paste does not work correctly when there were alias imports in the source file
- [`KT-10512`](https://youtrack.jetbrains.com/issue/KT-10512) Do not delete imports with unresolved parts when optimizing
- [`KT-13048`](https://youtrack.jetbrains.com/issue/KT-13048) "Strip trailing spaces on Save" should not strip trailing spaces inside multiline strings in Kotlin
- [`KT-17375`](https://youtrack.jetbrains.com/issue/KT-17375) Optimize Imports does not remove unused import alias
- [`KT-27385`](https://youtrack.jetbrains.com/issue/KT-27385) Uast: property references should resolve to getters/setters
- [`KT-28627`](https://youtrack.jetbrains.com/issue/KT-28627) Invalid detection of Kotlin jvmTarget inside Idea/gradle build
- [`KT-29267`](https://youtrack.jetbrains.com/issue/KT-29267) Enable ultra-light classes by default
- [`KT-29892`](https://youtrack.jetbrains.com/issue/KT-29892) A lot of threads are waiting in KotlinConfigurationCheckerComponent
- [`KT-30356`](https://youtrack.jetbrains.com/issue/KT-30356) Kotlin facet: all JVM 9+ target platforms are shown as "Target Platform = JVM 9" in Project Structure dialog
- [`KT-30514`](https://youtrack.jetbrains.com/issue/KT-30514) Auto-import with "Add unambiguous imports on the fly" imports enum members from another package
- [`KT-30583`](https://youtrack.jetbrains.com/issue/KT-30583) Kotlin light elements should be `isEquivalentTo` to it's origins
- [`KT-30688`](https://youtrack.jetbrains.com/issue/KT-30688) Memory leak in the PerModulePackageCacheService.onTooComplexChange method
- [`KT-30949`](https://youtrack.jetbrains.com/issue/KT-30949) Optimize Imports removes used import alias
- [`KT-30957`](https://youtrack.jetbrains.com/issue/KT-30957) Kotlin UAST: USimpleNameReferenceExpression in "imports" for class' member resolves incorrectly to class, not to the member
- [`KT-31090`](https://youtrack.jetbrains.com/issue/KT-31090) java.lang.NoSuchMethodError: org.jetbrains.kotlin.idea.UtilsKt.addModuleDependencyIfNeeded on import of a multiplatform project with Android target (191 IDEA + master)
- [`KT-31092`](https://youtrack.jetbrains.com/issue/KT-31092) Don't check all selected files in CheckComponentsUsageSearchAction.update()
- [`KT-31319`](https://youtrack.jetbrains.com/issue/KT-31319) False positive "Unused import" for `provideDelegate` extension
- [`KT-31332`](https://youtrack.jetbrains.com/issue/KT-31332) Kotlin AnnotatedElementsSearch does't support Kotlin `object`
- [`KT-31129`](https://youtrack.jetbrains.com/issue/KT-31129) Call only Kotlin-specific reference contributors for getting Kotlin references from PSI
- [`KT-31693`](https://youtrack.jetbrains.com/issue/KT-31693) Project with no Kotlin: JPS rebuild fails with NCDFE for GradleSettingsService at KotlinMPPGradleProjectTaskRunner.canRun()

### IDE. Completion

- [`KT-29038`](https://youtrack.jetbrains.com/issue/KT-29038) Autocomplete "suspend" into "suspend fun" at top level and class level (except in kts top level)
- [`KT-29398`](https://youtrack.jetbrains.com/issue/KT-29398) Add "arg" postfix template
- [`KT-30511`](https://youtrack.jetbrains.com/issue/KT-30511) Replace extra space after autocompleting data class with file name by parentheses

### IDE. Debugger

- [`KT-10636`](https://youtrack.jetbrains.com/issue/KT-10636) Debugger: can't evaluate call of function type parameter inside inline function
- [`KT-18247`](https://youtrack.jetbrains.com/issue/KT-18247) Debugger: class level watches fail to evaluate outside of class instance context
- [`KT-18263`](https://youtrack.jetbrains.com/issue/KT-18263) Settings / Debugger / Java Type Renderers: unqualified Kotlin class members in Java expressions are shown as errors
- [`KT-23586`](https://youtrack.jetbrains.com/issue/KT-23586) Non-trivial properties autocompletion in evaluation window
- [`KT-30216`](https://youtrack.jetbrains.com/issue/KT-30216) Evaluate expression: declarations annotated with Experimental (LEVEL.ERROR) fail due to compilation error
- [`KT-30610`](https://youtrack.jetbrains.com/issue/KT-30610) Debugger: Variables view shows second `this` instance for inline function even from the same class as caller function
- [`KT-30714`](https://youtrack.jetbrains.com/issue/KT-30714) Breakpoints are shown as invalid for classes that are not loaded yet
- [`KT-30934`](https://youtrack.jetbrains.com/issue/KT-30934) "InvocationException: Exception occurred in target VM" on debugger breakpoint hit (with kotlintest)
- [`KT-31266`](https://youtrack.jetbrains.com/issue/KT-31266) Kotlin debugger incompatibility with latest 192 nightly: KotlinClassWithDelegatedPropertyRenderer

### IDE. Gradle

- [`KT-29854`](https://youtrack.jetbrains.com/issue/KT-29854) File collection dependency does not work with NMPP+JPS
- [`KT-30531`](https://youtrack.jetbrains.com/issue/KT-30531) Gradle: NodeJS downloading
- [`KT-30767`](https://youtrack.jetbrains.com/issue/KT-30767) Kotlin import uses too much memory when working with big projects
- [`KT-29564`](https://youtrack.jetbrains.com/issue/KT-29564) kotlin.parallel.tasks.in.project=true causes idea to create kotlin modules with target JVM 1.6
- [`KT-31014`](https://youtrack.jetbrains.com/issue/KT-31014) Gradle, JS: Webpack watch mode

### IDE. Gradle. Script

- [`KT-30638`](https://youtrack.jetbrains.com/issue/KT-30638) "Highlighting in scripts is not available until all Script Dependencies are loaded" in Diff viewer
- [`KT-31124`](https://youtrack.jetbrains.com/issue/KT-31124) “compileKotlin - configuration not found: kotlinScriptDef, the plugin is probably applied by a mistake” after creating new project with IJ and Kotlin from master
- [`KT-30974`](https://youtrack.jetbrains.com/issue/KT-30974) Script dependencies resolution failed error while trying to use Kotlin for Gradle

### IDE. Hints

- [`KT-30057`](https://youtrack.jetbrains.com/issue/KT-30057) "View->Type info" shows "Type is unknown" for named argument syntax

### IDE. Inspections and Intentions

#### New Features

- [`KT-11629`](https://youtrack.jetbrains.com/issue/KT-11629) Inspection: creating Throwable without throwing it
- [`KT-12392`](https://youtrack.jetbrains.com/issue/KT-12392) Unused import with alias should be highlighted and removed with Optimize Imports
- [`KT-12721`](https://youtrack.jetbrains.com/issue/KT-12721) inspection should be made for converting Integer.toString(int) to int.toString()
- [`KT-13962`](https://youtrack.jetbrains.com/issue/KT-13962) Intention to replace Java collection constructor calls with function calls from stdlib (ArrayList() → arrayListOf())
- [`KT-15537`](https://youtrack.jetbrains.com/issue/KT-15537) Add inspection + intention to replace IntRange.start/endInclusive with first/last
- [`KT-21195`](https://youtrack.jetbrains.com/issue/KT-21195) ReplaceWith intention could save generic type arguments
- [`KT-25262`](https://youtrack.jetbrains.com/issue/KT-25262) Intention: Rename class to containing file name
- [`KT-25439`](https://youtrack.jetbrains.com/issue/KT-25439) Inspection "Map replaceable with EnumMap"
- [`KT-26269`](https://youtrack.jetbrains.com/issue/KT-26269) Inspection to replace associate with associateWith or associateBy
- [`KT-26629`](https://youtrack.jetbrains.com/issue/KT-26629) Inspection to replace `==` operator on Double.NaN with `equals` call
- [`KT-27411`](https://youtrack.jetbrains.com/issue/KT-27411) Inspection and Quickfix to replace System.exit() with exitProcess()
- [`KT-29344`](https://youtrack.jetbrains.com/issue/KT-29344) Convert property initializer to getter: suggest on property name
- [`KT-29666`](https://youtrack.jetbrains.com/issue/KT-29666) Quickfix for "DEPRECATED_JAVA_ANNOTATION": migrate arguments
- [`KT-29798`](https://youtrack.jetbrains.com/issue/KT-29798) Add 'Covariant equals' inspection
- [`KT-29799`](https://youtrack.jetbrains.com/issue/KT-29799) Inspection: class with non-null self-reference as a parameter in its primary constructor
- [`KT-30078`](https://youtrack.jetbrains.com/issue/KT-30078) Add "Add getter/setter" quick fix for uninitialized property
- [`KT-30381`](https://youtrack.jetbrains.com/issue/KT-30381) Inspection + quickfix to replace non-null assertion with return
- [`KT-30389`](https://youtrack.jetbrains.com/issue/KT-30389) Fix to convert argument to Int: suggest roundToInt()
- [`KT-30501`](https://youtrack.jetbrains.com/issue/KT-30501) Add inspection to replace filter { it is Foo } with filterIsInstance<Foo> and filter { it != null } with filterNotNull
- [`KT-30612`](https://youtrack.jetbrains.com/issue/KT-30612) Unused symbol inspection should detect enum entry
- [`KT-30663`](https://youtrack.jetbrains.com/issue/KT-30663) Fully qualified name is added on quick fix for original class name if import alias exists
- [`KT-30725`](https://youtrack.jetbrains.com/issue/KT-30725) Inspection which replaces `.sorted().first()` with `.min()`

#### Fixes

- [`KT-5412`](https://youtrack.jetbrains.com/issue/KT-5412) "Replace non-null assertion with `if` expression" should replace parent expression
- [`KT-13549`](https://youtrack.jetbrains.com/issue/KT-13549) "Package directive doesn't match file location" for root package
- [`KT-14040`](https://youtrack.jetbrains.com/issue/KT-14040) Secondary enum class constructor is marked as "unused" by IDE
- [`KT-18459`](https://youtrack.jetbrains.com/issue/KT-18459) Spring: "Autowiring for Bean Class (Kotlin)" inspection adds not working `@Named` annotation to property
- [`KT-21526`](https://youtrack.jetbrains.com/issue/KT-21526) used class is marked as "never used"
- [`KT-22896`](https://youtrack.jetbrains.com/issue/KT-22896) "Change function signature" quickfix on "x overrides nothing" doesn't rename type arguments
- [`KT-27089`](https://youtrack.jetbrains.com/issue/KT-27089) ReplaceWith quickfix doesn't take into account generic parameter
- [`KT-27821`](https://youtrack.jetbrains.com/issue/KT-27821) SimplifiableCallChain inspection quick fix removes comments for intermediate operations
- [`KT-28485`](https://youtrack.jetbrains.com/issue/KT-28485) Incorrect parameter name after running "Add parameter to function" intention when argument variable is upper case const
- [`KT-28619`](https://youtrack.jetbrains.com/issue/KT-28619) "Add braces to 'if' statement" moves end-of-line comment inside an `if` branch if statement inside `if` is block
- [`KT-29556`](https://youtrack.jetbrains.com/issue/KT-29556) "Remove redundant 'let' call" doesn't rename parameter with convention `invoke` call
- [`KT-29677`](https://youtrack.jetbrains.com/issue/KT-29677) "Specify type explicitly" intention produces invalid output for type escaped with backticks
- [`KT-29764`](https://youtrack.jetbrains.com/issue/KT-29764) "Convert property to function" intention doesn't warn about the property overloads at child class constructor
- [`KT-29812`](https://youtrack.jetbrains.com/issue/KT-29812) False positive for HasPlatformType with member extension on 'dynamic'
- [`KT-29869`](https://youtrack.jetbrains.com/issue/KT-29869) 'WhenWithOnlyElse': possibly useless inspection with false grey warning highlighting during editing the code
- [`KT-30038`](https://youtrack.jetbrains.com/issue/KT-30038) 'Remove redundant Unit" false positive when return type is nullable Unit
- [`KT-30082`](https://youtrack.jetbrains.com/issue/KT-30082) False positive "redundant `.let` call" for lambda functions stored in nullable references
- [`KT-30173`](https://youtrack.jetbrains.com/issue/KT-30173) "Nested lambda has shadowed implicit parameter" is suggested when both parameters are logically the same
- [`KT-30208`](https://youtrack.jetbrains.com/issue/KT-30208) Convert to anonymous object: lambda generic type argument is lost
- [`KT-30215`](https://youtrack.jetbrains.com/issue/KT-30215) No "surround with null" check is suggested for an assignment
- [`KT-30228`](https://youtrack.jetbrains.com/issue/KT-30228) 'Convert to also/apply/run/with' intention behaves differently depending on the position of infix function call
- [`KT-30457`](https://youtrack.jetbrains.com/issue/KT-30457) MoveVariableDeclarationIntoWhen: do not report gray warning on variable declarations taking multiple lines / containing preemptive returns
- [`KT-30481`](https://youtrack.jetbrains.com/issue/KT-30481) Do not report ImplicitNullableNothingType on a function/property that overrides a function/property of type 'Nothing?'
- [`KT-30527`](https://youtrack.jetbrains.com/issue/KT-30527) False positive "Type alias is never used" with import of enum member
- [`KT-30559`](https://youtrack.jetbrains.com/issue/KT-30559) Redundant Getter, Redundant Setter: reduce range to getter/setter header
- [`KT-30565`](https://youtrack.jetbrains.com/issue/KT-30565) False positive "Suspicious 'var' property" inspection with annotated default property getter
- [`KT-30579`](https://youtrack.jetbrains.com/issue/KT-30579) Kotlin-gradle groovy inspections should depend on Groovy plugin
- [`KT-30613`](https://youtrack.jetbrains.com/issue/KT-30613) "Convert to anonymous function" should not insert named argument when interoping with Java functions
- [`KT-30614`](https://youtrack.jetbrains.com/issue/KT-30614) String templates suggest removing curly braces for backtick escaped identifiers
- [`KT-30622`](https://youtrack.jetbrains.com/issue/KT-30622) Add names to call arguments starting from given argument
- [`KT-30637`](https://youtrack.jetbrains.com/issue/KT-30637) False positive "unused constructor" for local class
- [`KT-30669`](https://youtrack.jetbrains.com/issue/KT-30669) Import quick fix does not work for property/function with original name if import alias for them exist
- [`KT-30761`](https://youtrack.jetbrains.com/issue/KT-30761) Replace assert boolean with assert equality produces uncompilable code when compared arguments type are different
- [`KT-30769`](https://youtrack.jetbrains.com/issue/KT-30769) Override quickfix creates "sealed fun"
- [`KT-30833`](https://youtrack.jetbrains.com/issue/KT-30833) Exception after "Introduce Import Alias" if invoke in import
- [`KT-30876`](https://youtrack.jetbrains.com/issue/KT-30876) SimplifyNotNullAssert inspection changes semantics
- [`KT-30900`](https://youtrack.jetbrains.com/issue/KT-30900) Invert 'if' condition respects neither code formatting nor inline comments
- [`KT-30910`](https://youtrack.jetbrains.com/issue/KT-30910) "Use property access syntax" is not suitable as text for inspection problem text
- [`KT-30916`](https://youtrack.jetbrains.com/issue/KT-30916) Quickfix "Remove redundant qualifier name" can't work with user type with generic parameter
- [`KT-31103`](https://youtrack.jetbrains.com/issue/KT-31103) Don't invoke Gradle related inspections when Gradle plugin is disabled
- [`KT-31349`](https://youtrack.jetbrains.com/issue/KT-31349) Add name to argument should not be suggested for Java library classes
- [`KT-31404`](https://youtrack.jetbrains.com/issue/KT-31404) Redundant 'requireNotNull' or 'checkNotNull' inspection: don't remove first argument
- [`KT-25465`](https://youtrack.jetbrains.com/issue/KT-25465) "Redundant 'suspend' modifier" with suspend operator invoke
- [`KT-26337`](https://youtrack.jetbrains.com/issue/KT-26337) Exception (resource not found) in quick-fix tests in AS32
- [`KT-30879`](https://youtrack.jetbrains.com/issue/KT-30879) False positive "Redundant qualifier name"
- [`KT-31415`](https://youtrack.jetbrains.com/issue/KT-31415) UI hangs due to long computations for "Use property access syntax" intention with new inference
- [`KT-31441`](https://youtrack.jetbrains.com/issue/KT-31441) False positive "Remove explicit type arguments" inspection for projection type

### IDE. Libraries

- [`KT-30790`](https://youtrack.jetbrains.com/issue/KT-30790) Unstable IDE navigation behavior to `expect`/`actual` symbols in stdlib
- [`KT-30821`](https://youtrack.jetbrains.com/issue/KT-30821) K/N: Navigation downwards the hierarchy in stdlib source code opens to stubs

### IDE. Navigation

- [`KT-18322`](https://youtrack.jetbrains.com/issue/KT-18322) Find Usages not finding Java usage of @JvmField declared in primary constructor
- [`KT-27332`](https://youtrack.jetbrains.com/issue/KT-27332) Gutter icons are still shown even if disabled

### IDE. Refactorings

- [`KT-30471`](https://youtrack.jetbrains.com/issue/KT-30471) Make `KotlinElementActionsFactory.createChangeParametersActions` able to just add parameters

### IDE. Run Configurations

- [`KT-29352`](https://youtrack.jetbrains.com/issue/KT-29352) Kotlin + Java 11 + Windows : impossible to run applications with long command lines, even with dynamic.classpath=true

### IDE. Scratch

- [`KT-29642`](https://youtrack.jetbrains.com/issue/KT-29642) Once hidden, `Scratch Output` window wouldn't show the results unless the project is reopened

### IDE. Script

- [`KT-30295`](https://youtrack.jetbrains.com/issue/KT-30295) Resolver for 'completion/highlighting in  ScriptDependenciesSourceInfo...' does not know how to resolve [] or [Library(null)]
- [`KT-30690`](https://youtrack.jetbrains.com/issue/KT-30690) Highlighting for scripts in diff view doesn't work for left part

### IDE. Tests Support

- [`KT-30995`](https://youtrack.jetbrains.com/issue/KT-30995) Gradle test runner: "No tasks available" for a test class in non-MPP project

### IDE. Wizards

- [`KT-30645`](https://youtrack.jetbrains.com/issue/KT-30645) Update New Project Wizard templates related to Kotlin/JS

### JS. Tools

- [`KT-31563`](https://youtrack.jetbrains.com/issue/KT-31563) Gradle/JS: npmResolve fails with "Invalid version" when user project's version does not match npm rules
- [`KT-31566`](https://youtrack.jetbrains.com/issue/KT-31566) Gradle/JS: with explicit call to `nodejs { testTask { useNodeJs() } }` configuration fails : "Could not find which method to invoke"

## JavaScript

- [`KT-31007`](https://youtrack.jetbrains.com/issue/KT-31007) Kotlin/JS 1.3.30 - private method in an interface in the external library causes ReferenceError

### Libraries

- [`KT-30174`](https://youtrack.jetbrains.com/issue/KT-30174) Annotation for experimental stdlib API
- [`KT-30451`](https://youtrack.jetbrains.com/issue/KT-30451) Redundant call of selector in maxBy&minBy
- [`KT-30560`](https://youtrack.jetbrains.com/issue/KT-30560) Fix Throwable::addSuppressed from stdlib to make it work without stdlib-jdk7 in runtime
- [`KT-24810`](https://youtrack.jetbrains.com/issue/KT-24810) Support common string<->ByteArray UTF-8 conversion
- [`KT-29265`](https://youtrack.jetbrains.com/issue/KT-29265) String.toCharArray() is not available in common stdlib
- [`KT-31194`](https://youtrack.jetbrains.com/issue/KT-31194) assertFails and assertFailsWith don't work with suspend functions
- [`KT-31639`](https://youtrack.jetbrains.com/issue/KT-31639) 'Iterbale.drop' drops too much because of overflow

### Reflection

- [`KT-29041`](https://youtrack.jetbrains.com/issue/KT-29041) KAnnotatedElement should have an extension function to verify if certain annotation is present
- [`KT-30344`](https://youtrack.jetbrains.com/issue/KT-30344) Avoid using .kotlin_module in kotlin-reflect

### Tools. Android Extensions

- [`KT-30993`](https://youtrack.jetbrains.com/issue/KT-30993) Android Extensions: Make @Parcelize functionality non-experimental

### Tools. CLI

- [`KT-27638`](https://youtrack.jetbrains.com/issue/KT-27638) Add -Xjava-sources compiler argument to specify directories with .java source files which can be referenced from the compiled Kotlin sources
- [`KT-27778`](https://youtrack.jetbrains.com/issue/KT-27778) Add -Xpackage-prefix compiler argument to specify package prefix for Java sources resolution
- [`KT-30973`](https://youtrack.jetbrains.com/issue/KT-30973) Compilation on IBM J9 (build 2.9, JRE 1.8.0 AIX ppc64-64-Bit) fails unless -Xuse-javac is specified

### Tools. Compiler Plugins

- [`KT-30343`](https://youtrack.jetbrains.com/issue/KT-30343) Add new Quarkus preset to all-open compiler plugin

### Tools. Gradle

#### New Features

- [`KT-20156`](https://youtrack.jetbrains.com/issue/KT-20156) Publish the Kotlin Javascript Gradle plugin to the Gradle Plugins Portal
- [`KT-26256`](https://youtrack.jetbrains.com/issue/KT-26256) In new MPP, support Java compilation in JVM targets
- [`KT-27273`](https://youtrack.jetbrains.com/issue/KT-27273) Support the Gradle 'application' plugin in new MPP or provide an alternative
- [`KT-30528`](https://youtrack.jetbrains.com/issue/KT-30528) Gradle, JS tests: support basic builtin test runner
- [`KT-31015`](https://youtrack.jetbrains.com/issue/KT-31015) Gradle, JS: Change default for new kotlin-js and experimental kotlin-multiplatform plugins
- [`KT-30573`](https://youtrack.jetbrains.com/issue/KT-30573) Gradle, JS: enable source maps by default, change paths relative to node_modules directory
- [`KT-30747`](https://youtrack.jetbrains.com/issue/KT-30747) Gradle, JS tests: provide option to disable test configuration per target
- [`KT-31010`](https://youtrack.jetbrains.com/issue/KT-31010) Gradle, JS tests: Mocha
- [`KT-31011`](https://youtrack.jetbrains.com/issue/KT-31011) Gradle, JS tests: Karma
- [`KT-31013`](https://youtrack.jetbrains.com/issue/KT-31013) Gradle, JS: Webpack
- [`KT-31016`](https://youtrack.jetbrains.com/issue/KT-31016) Gradle: yarn downloading
- [`KT-31017`](https://youtrack.jetbrains.com/issue/KT-31017) Gradle, yarn: support workspaces

#### Fixes

- [`KT-13256`](https://youtrack.jetbrains.com/issue/KT-13256) CompileJava tasks in Kotlin2Js Gradle plugin
- [`KT-16355`](https://youtrack.jetbrains.com/issue/KT-16355) Rename "compileKotlin2Js" Gradle task to "compileKotlinJs"
- [`KT-26255`](https://youtrack.jetbrains.com/issue/KT-26255) Using the jvmWithJava preset in new MPP leads to counter-intuitive source set names and directory structure
- [`KT-27640`](https://youtrack.jetbrains.com/issue/KT-27640) Do not use `-Xbuild-file` when invoking the Kotlin compiler in Gradle plugins
- [`KT-29284`](https://youtrack.jetbrains.com/issue/KT-29284) kotlin2js plugin applies java plugin
- [`KT-30132`](https://youtrack.jetbrains.com/issue/KT-30132) Could not initialize class org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil on build by gradle
- [`KT-30596`](https://youtrack.jetbrains.com/issue/KT-30596) Kotlin Gradle Plugin: Forward stdout and stderr logger of out of process though gradle logger
- [`KT-31106`](https://youtrack.jetbrains.com/issue/KT-31106) Kotlin compilation fails with locked build script dependencies and Gradle 5
- [`KT-28985`](https://youtrack.jetbrains.com/issue/KT-28985) Java tests not executed in a module created with presets.jvmWithJava
- [`KT-30340`](https://youtrack.jetbrains.com/issue/KT-30340) kotlin("multiplatform") plugin is not working properly with Spring Boot
- [`KT-30784`](https://youtrack.jetbrains.com/issue/KT-30784) Deprecation warning "API 'variant.getPackageLibrary()' is obsolete and has been replaced with 'variant.getPackageLibraryProvider()'" for a multiplatform library with Android target
- [`KT-31027`](https://youtrack.jetbrains.com/issue/KT-31027) java.lang.NoSuchMethodError: No static method hashCode(Z)I in class Ljava/lang/Boolean; or its super classes (declaration of 'java.lang.Boolean' appears in /system/framework/core-libart.jar)

### Tools. Incremental Compile

- [`KT-31131`](https://youtrack.jetbrains.com/issue/KT-31131) Regression: incremental compilation of multi-file part throws exception

### Tools. J2K

- [`KT-23023`](https://youtrack.jetbrains.com/issue/KT-23023) J2K: Inspection to convert Arrays.copyOf(a, size) to a.copyOf(size)
- [`KT-26550`](https://youtrack.jetbrains.com/issue/KT-26550) J2K: Check context/applicability of conversion, don't suggest for libraries, jars, etc.
- [`KT-29568`](https://youtrack.jetbrains.com/issue/KT-29568) Disabled "Convert Java File to Kotlin File" action is shown in project view context menu for XML files

### Tools. Scripts

- [`KT-30986`](https://youtrack.jetbrains.com/issue/KT-30986) Missing dependencies when JSR-223 script engines are used from `kotlin-script-util`

### Tools. kapt

- [`KT-26203`](https://youtrack.jetbrains.com/issue/KT-26203) `kapt.use.worker.api=true` throws a NullPointerException on Java 10/11
- [`KT-30739`](https://youtrack.jetbrains.com/issue/KT-30739) Kapt generated sources are not visible from the IDE when "Create separate module per source set" is disabled
- [`KT-31064`](https://youtrack.jetbrains.com/issue/KT-31064) Periodically build crash when using incremental kapt
- [`KT-23880`](https://youtrack.jetbrains.com/issue/KT-23880) Kapt: Support incremental annotation processors
- [`KT-31322`](https://youtrack.jetbrains.com/issue/KT-31322) Kapt does not run annotation processing when sources change.

## Previous releases
This release also includes the fixes and improvements from the [previous releases](https://github.com/JetBrains/kotlin/releases/tag/v1.3.31).