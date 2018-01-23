# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.2.21

### Fixes

- [`KT-22349`](https://youtrack.jetbrains.com/issue/KT-22349) Android: creating new Basic activity fails with Throwable: "Inconsistent FILE tree in SingleRootFileViewProvider" at SingleRootFileViewProvider.checkLengthConsistency()
- [`KT-22459`](https://youtrack.jetbrains.com/issue/KT-22459) Remove .proto files from kotlin-reflect.jar

## 1.2.20

### Android

- [`KT-20085`](https://youtrack.jetbrains.com/issue/KT-20085) Android Extensions: ClassCastException after changing type of view in layout XML
- [`KT-20235`](https://youtrack.jetbrains.com/issue/KT-20235) Error, can't use plugin kotlin-android-extensions
- [`KT-20269`](https://youtrack.jetbrains.com/issue/KT-20269) Mark 'kapt.kotlin.generated' as a source root automatically in Android projects
- [`KT-20545`](https://youtrack.jetbrains.com/issue/KT-20545) Parcelable: Migrate to canonical NEW-DUP-INVOKESPECIAL form
- [`KT-20742`](https://youtrack.jetbrains.com/issue/KT-20742) @Serializable and @Parcelize do not work together
- [`KT-20928`](https://youtrack.jetbrains.com/issue/KT-20928) @Parcelize. Verify Error for Android Api 19

### Binary Metadata

- [`KT-11586`](https://youtrack.jetbrains.com/issue/KT-11586) Support class literal annotation arguments in AnnotationSerializer

### Compiler

#### New Features

- [`KT-17944`](https://youtrack.jetbrains.com/issue/KT-17944) Allow 'expect' final member be implemented by 'actual' open member
- [`KT-21982`](https://youtrack.jetbrains.com/issue/KT-21982) Recognize Checker Framework *declaration* annotations
- [`KT-17609`](https://youtrack.jetbrains.com/issue/KT-17609) Intrinsic suspend val coroutineContext
#### Performance Improvements

- [`KT-21322`](https://youtrack.jetbrains.com/issue/KT-21322) for-in-char-sequence loop improvements
- [`KT-21323`](https://youtrack.jetbrains.com/issue/KT-21323) Decreasing range loop improvements
#### Fixes

- [`KT-4174`](https://youtrack.jetbrains.com/issue/KT-4174) Verify error on lambda with closure in local class super call
- [`KT-10473`](https://youtrack.jetbrains.com/issue/KT-10473) Inapplicable diagnostics for mixed JS / JVM projects
- [`KT-12541`](https://youtrack.jetbrains.com/issue/KT-12541) VerifyError: Bad type on operand stack for local variable captured in local class
- [`KT-13454`](https://youtrack.jetbrains.com/issue/KT-13454) VerifyError on capture of outer class properties in closure inside inner class constructor
- [`KT-14148`](https://youtrack.jetbrains.com/issue/KT-14148) `VerifyError: Bad type on operand stack`  for anonymous type inheriting inner class
- [`KT-18254`](https://youtrack.jetbrains.com/issue/KT-18254) enumValueOf and enumValues throw UnsupportedOperationException when used within a non-inline function block
- [`KT-18514`](https://youtrack.jetbrains.com/issue/KT-18514) IllegalStateException on compile object that inherits its inner interface or class
- [`KT-18639`](https://youtrack.jetbrains.com/issue/KT-18639) VerifyError: Bad type on operand stack
- [`KT-19188`](https://youtrack.jetbrains.com/issue/KT-19188) Nondeterministic method order in class files using DefaultImpls
- [`KT-19827`](https://youtrack.jetbrains.com/issue/KT-19827) Strange VerifyError in simple Example
- [`KT-19928`](https://youtrack.jetbrains.com/issue/KT-19928) Analyze / Inspect Code: ISE "Concrete fake override public final fun <get-allowedTargets>()" at BridgesKt.findConcreteSuperDeclaration()
- [`KT-20433`](https://youtrack.jetbrains.com/issue/KT-20433) NPE during JVM code generation
- [`KT-20639`](https://youtrack.jetbrains.com/issue/KT-20639) Obsolete term "native" used in error message
- [`KT-20802`](https://youtrack.jetbrains.com/issue/KT-20802) USELESS_CAST diagnostic in functions with expression body
- [`KT-20873`](https://youtrack.jetbrains.com/issue/KT-20873) False CAST_NEVER_SUCCEEDS when upcasting Nothing
- [`KT-20903`](https://youtrack.jetbrains.com/issue/KT-20903) Method reference to expect function results in bogus resolution ambiguity
- [`KT-21105`](https://youtrack.jetbrains.com/issue/KT-21105) Compiler incorrectly optimize the operator `in`  with a floating point type range with NaN bound.
- [`KT-21146`](https://youtrack.jetbrains.com/issue/KT-21146) ArrayIndexOutOfBoundsException at org.jetbrains.kotlin.codegen.MemberCodegen.generateMethodCallTo(MemberCodegen.java:841)
- [`KT-21267`](https://youtrack.jetbrains.com/issue/KT-21267) Report pre-release errors if pre-release compiler is run with a release language version
- [`KT-21321`](https://youtrack.jetbrains.com/issue/KT-21321) for-in-array loop improvements
- [`KT-21343`](https://youtrack.jetbrains.com/issue/KT-21343) Compound assignment operator compiles incorrectly when LHS is a property imported from object
- [`KT-21354`](https://youtrack.jetbrains.com/issue/KT-21354) Inconsistent behavior of 'for-in-range' loop if range is an array variable modified in loop body
- [`KT-21532`](https://youtrack.jetbrains.com/issue/KT-21532) Enum constructor not found
- [`KT-21535`](https://youtrack.jetbrains.com/issue/KT-21535) SAM wrapper is not created for a value of functional type in delegating or super constructor call in secondary constructor
- [`KT-21671`](https://youtrack.jetbrains.com/issue/KT-21671) Inline sam wrapper during inline in another module
- [`KT-21919`](https://youtrack.jetbrains.com/issue/KT-21919) Invalid MethodParameters attribute generated for "$DefaultImpls" synthetic class with javaParameters=true
- [`KT-20429`](https://youtrack.jetbrains.com/issue/KT-20429) False-positive 'Unused return value of a function with lambda expression body' in enum constant constructor
- [`KT-21827`](https://youtrack.jetbrains.com/issue/KT-21827) SMAP problem during default lambda parameter inline

### IDE

#### New Features

- [`KT-4001`](https://youtrack.jetbrains.com/issue/KT-4001) Allow to set arguments indent to 1 tab (currently two and not customized)
- [`KT-13378`](https://youtrack.jetbrains.com/issue/KT-13378) Provide ability to configure highlighting for !! in expressions and ? in types
- [`KT-17928`](https://youtrack.jetbrains.com/issue/KT-17928) Support code folding for primary constructors
- [`KT-20591`](https://youtrack.jetbrains.com/issue/KT-20591) Show @StringRes/@IntegerRes annotations in parameter info
- [`KT-20952`](https://youtrack.jetbrains.com/issue/KT-20952) "Navigate | Related symbol" should support expect/actual navigation
- [`KT-21229`](https://youtrack.jetbrains.com/issue/KT-21229) Make it possible to explicitly select "latest" language/API version
- [`KT-21469`](https://youtrack.jetbrains.com/issue/KT-21469) Wrap property initializers after equals sign
- [`KT-14670`](https://youtrack.jetbrains.com/issue/KT-14670) Support kotlinPackageName() macro in live templates
- [`KT-14951`](https://youtrack.jetbrains.com/issue/KT-14951) Editor: navigate actions could be available in intention menu (as done in Java)
- [`KT-15320`](https://youtrack.jetbrains.com/issue/KT-15320) Live templates: Add function which returns the "outer" class name
- [`KT-20067`](https://youtrack.jetbrains.com/issue/KT-20067) Return label hints
- [`KT-20533`](https://youtrack.jetbrains.com/issue/KT-20533) Show "this" and "it" type hints in lambdas.
- [`KT-20614`](https://youtrack.jetbrains.com/issue/KT-20614) Change location of initial parameter type hint when parameters are on multiple lines
- [`KT-21949`](https://youtrack.jetbrains.com/issue/KT-21949) Please add a separate Color Scheme settings for properties synthesized from Java accessors
- [`KT-21974`](https://youtrack.jetbrains.com/issue/KT-21974) Editor color scheme option for Kotlin typealias names
#### Performance Improvements

- [`KT-17367`](https://youtrack.jetbrains.com/issue/KT-17367) Rebuild requested for index KotlinJavaScriptMetaFileIndex 
- [`KT-21632`](https://youtrack.jetbrains.com/issue/KT-21632) Freezing on typing
- [`KT-21701`](https://youtrack.jetbrains.com/issue/KT-21701) IDEA 2017.3 high CPU usage
#### Fixes

- [`KT-9562`](https://youtrack.jetbrains.com/issue/KT-9562) Wrong indent after Enter after an annotation
- [`KT-12176`](https://youtrack.jetbrains.com/issue/KT-12176) Formatter could reformat long primary constructors
- [`KT-12862`](https://youtrack.jetbrains.com/issue/KT-12862) Formatting: Weird wrapping setting for long ?: operator
- [`KT-15099`](https://youtrack.jetbrains.com/issue/KT-15099) Odd code formatting when chaining lambdas and splitting lines on operators
- [`KT-15254`](https://youtrack.jetbrains.com/issue/KT-15254) Use Platform icons for "Run" icon in gutter
- [`KT-17254`](https://youtrack.jetbrains.com/issue/KT-17254) Remove obsolete unfold-icons in structure view 
- [`KT-17838`](https://youtrack.jetbrains.com/issue/KT-17838) Can't report exceptions from the Kotlin plugin 1.1.4-dev-119 in IDEA #IU-171.4424.37
- [`KT-17843`](https://youtrack.jetbrains.com/issue/KT-17843) Don't show parameter name hints when calling Java methods with unknown parameter names
- [`KT-17964`](https://youtrack.jetbrains.com/issue/KT-17964) Local variable type hints in editor for anonymous object
- [`KT-17965`](https://youtrack.jetbrains.com/issue/KT-17965) Do not shown argument name hints for assert
- [`KT-18829`](https://youtrack.jetbrains.com/issue/KT-18829) Do not show parameter name hints for mapOf
- [`KT-18839`](https://youtrack.jetbrains.com/issue/KT-18839) Semantic highlighting not work for local variables in init
- [`KT-19012`](https://youtrack.jetbrains.com/issue/KT-19012) Data Flow from here: doesn't find template usages
- [`KT-19017`](https://youtrack.jetbrains.com/issue/KT-19017) Data Flow from here doesn't find usage in range position of for cycle
- [`KT-19018`](https://youtrack.jetbrains.com/issue/KT-19018) Data Flow from here doesn't find any usages of for-variable
- [`KT-19036`](https://youtrack.jetbrains.com/issue/KT-19036) Data Flow from here: please find calls of extension too
- [`KT-19039`](https://youtrack.jetbrains.com/issue/KT-19039) Data Flow from here: please find cases when an investigated variable is transferred as a parameter into a library function
- [`KT-19087`](https://youtrack.jetbrains.com/issue/KT-19087) Data flow to here: usages with explicit receiver are not found
- [`KT-19089`](https://youtrack.jetbrains.com/issue/KT-19089) Data Flow to here: assigned values are not found if an investigated property is a delegated one
- [`KT-19104`](https://youtrack.jetbrains.com/issue/KT-19104) Data Flow from here: usage of parameter or variable not found when used as lambda receiver/parameter
- [`KT-19106`](https://youtrack.jetbrains.com/issue/KT-19106) Data Flow from here: show point of call of a function used as a parameter investigated parameter/variable
- [`KT-19112`](https://youtrack.jetbrains.com/issue/KT-19112) Data Flow to here for a function (or its return value) doesn't find shorten forms of assignments
- [`KT-19519`](https://youtrack.jetbrains.com/issue/KT-19519) Structure view is not updated properly for function classes
- [`KT-19727`](https://youtrack.jetbrains.com/issue/KT-19727) Code style: New line after '(' with anonymous object or multi-line lambda unexpected behavior
- [`KT-19820`](https://youtrack.jetbrains.com/issue/KT-19820) Strange highlightning for enum constructor
- [`KT-19823`](https://youtrack.jetbrains.com/issue/KT-19823) Kotlin Gradle project import into IntelliJ: import kapt generated classes into classpath
- [`KT-19824`](https://youtrack.jetbrains.com/issue/KT-19824) Please provide a separate icon for a common library
- [`KT-19915`](https://youtrack.jetbrains.com/issue/KT-19915) TODO calls not blue highlighted in lambdas/DSLs
- [`KT-20096`](https://youtrack.jetbrains.com/issue/KT-20096) Kotlin Gradle script: SOE after beginning of Pair definition before some script section
- [`KT-20314`](https://youtrack.jetbrains.com/issue/KT-20314) Kotlin formatter does not respect annotations code style settings
- [`KT-20329`](https://youtrack.jetbrains.com/issue/KT-20329) Multiplatform: gutter "Is subclassed by" should show expect subclass from the common module
- [`KT-20380`](https://youtrack.jetbrains.com/issue/KT-20380) Configure Kotlin plugin updates dialog does not open without opened project
- [`KT-20521`](https://youtrack.jetbrains.com/issue/KT-20521) Kotlin Gradle script: valid build.gradle.kts is red and becomes normal only after reopening the project
- [`KT-20603`](https://youtrack.jetbrains.com/issue/KT-20603) Facet import: when API version > language version, set API version = language version, not to 1.0
- [`KT-20782`](https://youtrack.jetbrains.com/issue/KT-20782) Non-atomic trees update
- [`KT-20813`](https://youtrack.jetbrains.com/issue/KT-20813) SAM with receiver: call with SAM usage is compiled with Gradle, but not with JPS
- [`KT-20880`](https://youtrack.jetbrains.com/issue/KT-20880) Add documentation quick fix should create multiline comment and place caret in right place
- [`KT-20883`](https://youtrack.jetbrains.com/issue/KT-20883) Provide more information in "Missing documentation" inspection message
- [`KT-20884`](https://youtrack.jetbrains.com/issue/KT-20884) Functions with receivers should allow [this] in KDoc
- [`KT-20937`](https://youtrack.jetbrains.com/issue/KT-20937) Exception thrown on RMB click on folder in Kotlin project
- [`KT-20938`](https://youtrack.jetbrains.com/issue/KT-20938) IDE: kotlinc.xml with KotlinCommonCompilerArguments/freeArgs: XSE: "Cannot deserialize class CommonCompilerArguments$DummyImpl" at BaseKotlinCompilerSettings.loadState()
- [`KT-20953`](https://youtrack.jetbrains.com/issue/KT-20953) "Choose actual" popup shows redundant information
- [`KT-20985`](https://youtrack.jetbrains.com/issue/KT-20985) Additional reimport is required in 2017.3/2018.1 idea after creating or importing mp project
- [`KT-20987`](https://youtrack.jetbrains.com/issue/KT-20987) (PerModulePackageCache miss) ISE: diagnoseMissingPackageFragment
- [`KT-21002`](https://youtrack.jetbrains.com/issue/KT-21002) "Highlight usages of identifier under caret" should work for "it"
- [`KT-21076`](https://youtrack.jetbrains.com/issue/KT-21076) Recursive Companion.ivoke() call should be marked with according icon
- [`KT-21132`](https://youtrack.jetbrains.com/issue/KT-21132) containsKey() in SoftValueMap considered pointless
- [`KT-21150`](https://youtrack.jetbrains.com/issue/KT-21150) Do not infer compiler version from build.txt
- [`KT-21200`](https://youtrack.jetbrains.com/issue/KT-21200) Improve Structure-view for Kotlin files
- [`KT-21214`](https://youtrack.jetbrains.com/issue/KT-21214) Fix funcion selection in kotlin
- [`KT-21275`](https://youtrack.jetbrains.com/issue/KT-21275) Don't show argument name hints in calls of methods on 'dynamic' type
- [`KT-21318`](https://youtrack.jetbrains.com/issue/KT-21318) Highlighting of function exit points does not work if the function is a getter for property
- [`KT-21363`](https://youtrack.jetbrains.com/issue/KT-21363) IDE: kotlinc.xml with KotlinCommonCompilerArguments: build fails with UOE: "Operation is not supported for read-only collection" at EmptyList.clear()
- [`KT-21409`](https://youtrack.jetbrains.com/issue/KT-21409) UAST: Super-call arguments are not modeled/visited
- [`KT-21418`](https://youtrack.jetbrains.com/issue/KT-21418) Gradle based project in IDEA 181: Kotlin facets are not created
- [`KT-21441`](https://youtrack.jetbrains.com/issue/KT-21441) Folding multiline strings adds a space at the start if there is not one.
- [`KT-21546`](https://youtrack.jetbrains.com/issue/KT-21546) java.lang.IllegalArgumentException: Unexpected container fatal IDE error
- [`KT-21575`](https://youtrack.jetbrains.com/issue/KT-21575) Secondary constructor call body is missing
- [`KT-21645`](https://youtrack.jetbrains.com/issue/KT-21645) Weird parameter hint position
- [`KT-21733`](https://youtrack.jetbrains.com/issue/KT-21733) Structure view is not updated
- [`KT-21756`](https://youtrack.jetbrains.com/issue/KT-21756) Find Usages for "type" in ts2kt provokes exception
- [`KT-21770`](https://youtrack.jetbrains.com/issue/KT-21770) Pasting $this into an interpolated string shouldn't escape $
- [`KT-21833`](https://youtrack.jetbrains.com/issue/KT-21833) Type hints shown when destructing triple with type parameters
- [`KT-21852`](https://youtrack.jetbrains.com/issue/KT-21852) Custom API version is lost when settings are reopen after restarting IDE
- [`KT-11503`](https://youtrack.jetbrains.com/issue/KT-11503) cmd+shift+enter action in .kt files does not work on empty lines
- [`KT-17217`](https://youtrack.jetbrains.com/issue/KT-17217) Navigate to symbol: hard to choose between a lot of extension overloads
- [`KT-18674`](https://youtrack.jetbrains.com/issue/KT-18674) Join Lines should join strings
- [`KT-19524`](https://youtrack.jetbrains.com/issue/KT-19524) "Local variable type hints" should respect static imports
- [`KT-21010`](https://youtrack.jetbrains.com/issue/KT-21010) Gutter "Is subclassed by" should show actual subclass from the all platform modules in IDEA 2017.3/2018.1
- [`KT-21036`](https://youtrack.jetbrains.com/issue/KT-21036) Throwable “Access is allowed from event dispatch thread only.” after creating nine similar classes with functions.
- [`KT-21213`](https://youtrack.jetbrains.com/issue/KT-21213) Multiline kdoc - intellij joins lines together without space
- [`KT-21592`](https://youtrack.jetbrains.com/issue/KT-21592) <args>-Xjsr305=strict</args> not taken into account during the kotlin files compilation in Idea (maven)
- [`KT-22050`](https://youtrack.jetbrains.com/issue/KT-22050) Redundant parameter type hint on SAM
- [`KT-22071`](https://youtrack.jetbrains.com/issue/KT-22071) Formatter insists on increasing indentation in forEach lambda
- [`KT-22093`](https://youtrack.jetbrains.com/issue/KT-22093) Unnecessary line wrap with new Kotlin code style
- [`KT-22111`](https://youtrack.jetbrains.com/issue/KT-22111) Error while indexing PsiPlainTextFileImpl cannot be cast to KtFile
- [`KT-22121`](https://youtrack.jetbrains.com/issue/KT-22121) Enter in empty argument list should apply normal indent if "Continuation indent for argument list" is off
- [`KT-21702`](https://youtrack.jetbrains.com/issue/KT-21702) `KtLightAnnotation` can't be converted to UAST
- [`KT-19900`](https://youtrack.jetbrains.com/issue/KT-19900) IntelliJ does not recognise no-arg "invokeInitializers" set in pom.xml
- [`KT-22227`](https://youtrack.jetbrains.com/issue/KT-22227) IDE + Gradle: compiler plugin settings are not imported
### IDE. Completion

- [`KT-13220`](https://youtrack.jetbrains.com/issue/KT-13220) Completion for non-primary-constructor properties should suggest names with types instead of types
- [`KT-12797`](https://youtrack.jetbrains.com/issue/KT-12797) Code completion does not work for inner in base class
- [`KT-16402`](https://youtrack.jetbrains.com/issue/KT-16402) AssertionError on completing expression after template in string literal
- [`KT-20166`](https://youtrack.jetbrains.com/issue/KT-20166) Completion: property declaration completion should be greedy if `tab` pressed
- [`KT-20506`](https://youtrack.jetbrains.com/issue/KT-20506) Second smart completion suggests the same value recursively

### IDE. Debugger

- [`KT-17514`](https://youtrack.jetbrains.com/issue/KT-17514) Debugger, evaluate value: cannot find local variable error on attempt to evaluate outer variable
- [`KT-20962`](https://youtrack.jetbrains.com/issue/KT-20962) NullPointerException because of nullable location in debugger
- [`KT-21538`](https://youtrack.jetbrains.com/issue/KT-21538) "Step into" method doesn't work after adding lambda parameter to the call
- [`KT-21820`](https://youtrack.jetbrains.com/issue/KT-21820) Debugger: Evaluation fails for instance properties (older Android SDKs)

### IDE. Inspections and Intentions

#### New Features

- [`KT-4580`](https://youtrack.jetbrains.com/issue/KT-4580) Intention + inspection to convert between explicit and implicit 'this'
- [`KT-11023`](https://youtrack.jetbrains.com/issue/KT-11023) Inspection to highlight usages of Collections.sort() and replace them with .sort() method from Kotlin stdlib
- [`KT-13702`](https://youtrack.jetbrains.com/issue/KT-13702) Issue a warning when equals is called recursively within itself
- [`KT-18449`](https://youtrack.jetbrains.com/issue/KT-18449) Multiplatform project: provide a quick fix "Implement methods" for a impl class
- [`KT-18828`](https://youtrack.jetbrains.com/issue/KT-18828) Provide an intention action to move a companion object member to top level
- [`KT-19103`](https://youtrack.jetbrains.com/issue/KT-19103) Inspection to remove unnecessary suspend modifier
- [`KT-20484`](https://youtrack.jetbrains.com/issue/KT-20484) Add quick fix to add required target to annotation used on a type
- [`KT-20492`](https://youtrack.jetbrains.com/issue/KT-20492) Offer "Simplify" intention for 'when' expression where only one branch is known to be true
- [`KT-20615`](https://youtrack.jetbrains.com/issue/KT-20615) Inspection to detect usages of values incorrectly marked by Kotlin as const from Java code
- [`KT-20631`](https://youtrack.jetbrains.com/issue/KT-20631) Inspection to detect use of Unit as a standalone expression
- [`KT-20644`](https://youtrack.jetbrains.com/issue/KT-20644) Warning for missing const paired with val modifier for primitives and strings
- [`KT-20714`](https://youtrack.jetbrains.com/issue/KT-20714) Inspection for self-assigment of properties
- [`KT-21023`](https://youtrack.jetbrains.com/issue/KT-21023) Inspection to highlight variables / functions with implicit `Nothing?` type
- [`KT-21510`](https://youtrack.jetbrains.com/issue/KT-21510) Add inspection to add/remove this to/from bound callable
- [`KT-21560`](https://youtrack.jetbrains.com/issue/KT-21560) Inspection to sort modifiers
- [`KT-21573`](https://youtrack.jetbrains.com/issue/KT-21573) Code Style Inspection: `to -> Pair` function used not in infix form 
- [`KT-16260`](https://youtrack.jetbrains.com/issue/KT-16260) Add intention to specify all types explicitly in destructuring assignment
- [`KT-21547`](https://youtrack.jetbrains.com/issue/KT-21547) Allow separate regex for test class and function names in IDE inspection
- [`KT-21741`](https://youtrack.jetbrains.com/issue/KT-21741) Inspection to detect is checks for object types
- [`KT-21950`](https://youtrack.jetbrains.com/issue/KT-21950) Enable quick-fixes for annotator-reported problems in "Inspect Code"
- [`KT-22103`](https://youtrack.jetbrains.com/issue/KT-22103) SortModifiersInspection should report annotations after modifiers
#### Fixes

- [`KT-15941`](https://youtrack.jetbrains.com/issue/KT-15941) "Convert to secondary constructor" produces invalid code for generic property with default value
- [`KT-16340`](https://youtrack.jetbrains.com/issue/KT-16340) "Unused receiver parameter" for invoke operator on companion object
- [`KT-17161`](https://youtrack.jetbrains.com/issue/KT-17161) IDE suggest to replace a for loop with `forEach` to aggresively
- [`KT-17332`](https://youtrack.jetbrains.com/issue/KT-17332) Intention to replace forEach with a 'for' loop should convert return@forEach to continue
- [`KT-17730`](https://youtrack.jetbrains.com/issue/KT-17730) Incorrect suggestion to replace loop with negation to `any{}`
- [`KT-18816`](https://youtrack.jetbrains.com/issue/KT-18816) IDEA suggests replacing for-in-range with stdlib operations
- [`KT-18881`](https://youtrack.jetbrains.com/issue/KT-18881) Invalid "Loop can be replaced with stdlib operations" warning when class has `add()` function
- [`KT-19560`](https://youtrack.jetbrains.com/issue/KT-19560) Do not warn about receiver parameter not used for companion object
- [`KT-19977`](https://youtrack.jetbrains.com/issue/KT-19977) Convert Lambda to reference produces red code when wrong implicit receiver is in scope
- [`KT-20091`](https://youtrack.jetbrains.com/issue/KT-20091) "Convert object literal to class" should create inner class if necessary
- [`KT-20300`](https://youtrack.jetbrains.com/issue/KT-20300) "Variable can be inlined" should not be suggested if there's a variable with the same name in nested scope
- [`KT-20349`](https://youtrack.jetbrains.com/issue/KT-20349) Convert lambda to reference for trailing lambda inserts parameter names for all arguments if at least one named argument was passed
- [`KT-20435`](https://youtrack.jetbrains.com/issue/KT-20435) False "function is never used" warning
- [`KT-20622`](https://youtrack.jetbrains.com/issue/KT-20622) Don't propose “Remove explicit type specification” when it can change semantic?
- [`KT-20763`](https://youtrack.jetbrains.com/issue/KT-20763) Wrong resulting code for "add star projection" quick-fix for inner class with generic outer one
- [`KT-20887`](https://youtrack.jetbrains.com/issue/KT-20887) Missing documentation warning shouldn't be triggered for a member of a private class
- [`KT-20888`](https://youtrack.jetbrains.com/issue/KT-20888) Documentation should be inherited from Map.Entry type
- [`KT-20889`](https://youtrack.jetbrains.com/issue/KT-20889) Members of anonymous objects should be treated as private and not trigger "Missing documentation" warning
- [`KT-20894`](https://youtrack.jetbrains.com/issue/KT-20894) "Add type" quick fix does not take into account `vararg` modifier
- [`KT-20901`](https://youtrack.jetbrains.com/issue/KT-20901) IntelliJ autocorrect to add parameter to data class constructor should make the parameter a val
- [`KT-20981`](https://youtrack.jetbrains.com/issue/KT-20981) False positive for "redundant super" in data class
- [`KT-21025`](https://youtrack.jetbrains.com/issue/KT-21025) Kotlin UAST violates `JvmDeclarationUElement` contract by employing `JavaUAnnotation`
- [`KT-21061`](https://youtrack.jetbrains.com/issue/KT-21061) Cant work with UElement.kt in IDEA with 1.2.0-rc-39: "Stub index points to a file without PSI"
- [`KT-21104`](https://youtrack.jetbrains.com/issue/KT-21104) Do not propose to make local lateinit var immutable
- [`KT-21122`](https://youtrack.jetbrains.com/issue/KT-21122) QuickFix to create member for expect class shouldn't add body
- [`KT-21159`](https://youtrack.jetbrains.com/issue/KT-21159) Fix signature invoked from Java breaks Kotlin code
- [`KT-21179`](https://youtrack.jetbrains.com/issue/KT-21179) Remove empty class body on companion object breaks code
- [`KT-21192`](https://youtrack.jetbrains.com/issue/KT-21192) Confusing "unused expression"
- [`KT-21237`](https://youtrack.jetbrains.com/issue/KT-21237) ReplaceWith incorrectly removes property assignment
- [`KT-21332`](https://youtrack.jetbrains.com/issue/KT-21332) Create from usage: do not propose to create abstract function in non-abstract class
- [`KT-21373`](https://youtrack.jetbrains.com/issue/KT-21373) 'Remove redundant let' quickfix does not work with `in`
- [`KT-21497`](https://youtrack.jetbrains.com/issue/KT-21497) Inspection considers if block to be a lambda
- [`KT-21544`](https://youtrack.jetbrains.com/issue/KT-21544) "Add type" quick fix incorrectly processes `vararg` modifier with primitive type array initializer
- [`KT-21603`](https://youtrack.jetbrains.com/issue/KT-21603) "Join declaration and assignment" should remove 'lateinit' for 'var'
- [`KT-21612`](https://youtrack.jetbrains.com/issue/KT-21612) The "Remove redundant getter" inspection removes the type specifier
- [`KT-21698`](https://youtrack.jetbrains.com/issue/KT-21698) `Create interface` shouldn't suggest to declare it inside a class which implements it
- [`KT-21726`](https://youtrack.jetbrains.com/issue/KT-21726) "arrayOf can be replaced with literal" inspection quick fix produces incompilable result in presence of spread operator
- [`KT-21727`](https://youtrack.jetbrains.com/issue/KT-21727) "Redundant spread operator" inspection does not report array literal
- [`KT-12814`](https://youtrack.jetbrains.com/issue/KT-12814) Specify type explicitly produces erroneous code when platform type overrides not-null type
- [`KT-15180`](https://youtrack.jetbrains.com/issue/KT-15180) Incorrect quickfix 'Specify type explicitly'
- [`KT-17816`](https://youtrack.jetbrains.com/issue/KT-17816) "Replace elvis with if" produce nasty code when safe casts are involved
- [`KT-18396`](https://youtrack.jetbrains.com/issue/KT-18396) Bad quickfix for wrong nested classes in inner class
- [`KT-19073`](https://youtrack.jetbrains.com/issue/KT-19073) No-op quick fix for "Convert lambda to reference" IDE suggestion 
- [`KT-19283`](https://youtrack.jetbrains.com/issue/KT-19283) Kotlin KProperty reference cannot be converted to lambda
- [`KT-19736`](https://youtrack.jetbrains.com/issue/KT-19736) Rephrase text in the unconventional property name inspection
- [`KT-19771`](https://youtrack.jetbrains.com/issue/KT-19771) Preserve old "Convert to expression body" range
- [`KT-20437`](https://youtrack.jetbrains.com/issue/KT-20437) Naming convetions inspection: Add separate inspection for top-level and object properties
- [`KT-20620`](https://youtrack.jetbrains.com/issue/KT-20620) Replace operator with function call breaks code
- [`KT-21414`](https://youtrack.jetbrains.com/issue/KT-21414) OverridersSearch attempts to create nameless fake light method
- [`KT-21780`](https://youtrack.jetbrains.com/issue/KT-21780) Wrong redundant setter inspection
- [`KT-21837`](https://youtrack.jetbrains.com/issue/KT-21837) Don't require documentation on tests and test classes
- [`KT-21929`](https://youtrack.jetbrains.com/issue/KT-21929) Inappropriate quick fix for a sealed class instantiation 
- [`KT-21983`](https://youtrack.jetbrains.com/issue/KT-21983) Do not suggest to remove explicit Unit type for expression body
- [`KT-16619`](https://youtrack.jetbrains.com/issue/KT-16619) Incorrect 'accessing non-final property in constructor' warning

### IDE. Refactorings

#### New Features

- [`KT-20095`](https://youtrack.jetbrains.com/issue/KT-20095) Allow conversion of selected companion methods to methods with @JvmStatic
#### Fixes

- [`KT-15840`](https://youtrack.jetbrains.com/issue/KT-15840) Introduce type alias: don't change not-nullable type with nullable typealias
- [`KT-17212`](https://youtrack.jetbrains.com/issue/KT-17212) Refactor / Inline Function: with 1 occurrence both "Inline all" and "Inline this only" are suggested
- [`KT-18594`](https://youtrack.jetbrains.com/issue/KT-18594) Refactor / Extract (Functional) Parameter are available for annotation arguments, but fail with AE: "Body element is not found"
- [`KT-20146`](https://youtrack.jetbrains.com/issue/KT-20146) IAE “parameter 'name' of NameUtil.splitNameIntoWords must not be null” at renaming class
- [`KT-20335`](https://youtrack.jetbrains.com/issue/KT-20335) Refactor → Extract Type Parameter: “AWT events are not allowed inside write action” after processing duplicates
- [`KT-20402`](https://youtrack.jetbrains.com/issue/KT-20402) Throwable “PsiElement(IDENTIFIER) by KotlinInplaceParameterIntroducer” on calling Refactor → Extract Parameter for default values
- [`KT-20403`](https://youtrack.jetbrains.com/issue/KT-20403) AE “Body element is not found” on calling Refactor → Extract Parameter for default values in constructor of class without body
- [`KT-20790`](https://youtrack.jetbrains.com/issue/KT-20790) Refactoring extension function/property overagressive
- [`KT-20766`](https://youtrack.jetbrains.com/issue/KT-20766) Typealias end-of-line is removed when moving function and typealias to new file
- [`KT-21071`](https://youtrack.jetbrains.com/issue/KT-21071) Cannot invoke move refactoring on a typealias
- [`KT-21162`](https://youtrack.jetbrains.com/issue/KT-21162) Adding parameters to kotlin data class leads to compilation error
- [`KT-21288`](https://youtrack.jetbrains.com/issue/KT-21288) Change Signature refactoring fails to change signature of overriders
- [`KT-21334`](https://youtrack.jetbrains.com/issue/KT-21334) Extract variable doesn't take into account the receiver of a bound callable reference
- [`KT-21371`](https://youtrack.jetbrains.com/issue/KT-21371) Rename refactoring sometimes erases identifier being renamed when popping up name proposals
- [`KT-21530`](https://youtrack.jetbrains.com/issue/KT-21530) KNPE in introduce variable
- [`KT-21508`](https://youtrack.jetbrains.com/issue/KT-21508) `java.lang.AssertionError: PsiLiteralExpression` on property safe delete in Idea 173 
- [`KT-21536`](https://youtrack.jetbrains.com/issue/KT-21536) Rename refactoring sometimes doesn't quite work
- [`KT-21604`](https://youtrack.jetbrains.com/issue/KT-21604) Rename package missing title
- [`KT-21963`](https://youtrack.jetbrains.com/issue/KT-21963) Refactor / Inline Property: "null" in place of number of occurrences of local variable references
- [`KT-21964`](https://youtrack.jetbrains.com/issue/KT-21964) Refactor / Inline: on declaration of element with one usage "Inline and keep" choice is not suggested
- [`KT-21965`](https://youtrack.jetbrains.com/issue/KT-21965) Refactor / Inline: wording in dialog could be unified
### JavaScript

#### New Features

- [`KT-20210`](https://youtrack.jetbrains.com/issue/KT-20210) [JS] Ultra-fast builds for development
#### Performance Improvements

- [`KT-2218`](https://youtrack.jetbrains.com/issue/KT-2218) JS: Optimise in checks for number ranges
- [`KT-20932`](https://youtrack.jetbrains.com/issue/KT-20932) JS: Make withIndex() on arrays intrinsic
- [`KT-21160`](https://youtrack.jetbrains.com/issue/KT-21160) JS: generate switch statement for when statement when possible
#### Fixes

- [`KT-7653`](https://youtrack.jetbrains.com/issue/KT-7653) JS: TypeError when try to access to "simple" property (w/o backing field at runtime)
- [`KT-18963`](https://youtrack.jetbrains.com/issue/KT-18963) javascript project: No output directory found for Module 'xxx_test' production on JPS compiling
- [`KT-19290`](https://youtrack.jetbrains.com/issue/KT-19290) JS integer overflow for unaryMinus
- [`KT-19826`](https://youtrack.jetbrains.com/issue/KT-19826) JS: don't remove debugger statement from suspend functions
- [`KT-20580`](https://youtrack.jetbrains.com/issue/KT-20580) JS: JSON.stringify could improve 'replacer' argument handling
- [`KT-20694`](https://youtrack.jetbrains.com/issue/KT-20694) JS: add missed parts to JS Date
- [`KT-20737`](https://youtrack.jetbrains.com/issue/KT-20737) JS: cache KProperty instances that used to access to delegated property
- [`KT-20738`](https://youtrack.jetbrains.com/issue/KT-20738) JS: remove useless calls to constructor of KProperty* (PropertyMetadata) when it generated for access to delegated property
- [`KT-20854`](https://youtrack.jetbrains.com/issue/KT-20854) `val` parameters of type `kotlin.Char` aren't boxed
- [`KT-20898`](https://youtrack.jetbrains.com/issue/KT-20898) JS: inline js with `for` without initializer causes compiiler to crash
- [`KT-20905`](https://youtrack.jetbrains.com/issue/KT-20905) JS: compiler crashes on invalid inline JavaScript code instead of reporting error
- [`KT-20908`](https://youtrack.jetbrains.com/issue/KT-20908) JS frontend crashes on uncompleted call to function with reified parameters
- [`KT-20978`](https://youtrack.jetbrains.com/issue/KT-20978) JS: inline doesn't work for Array's constructor when it called through typealias
- [`KT-20994`](https://youtrack.jetbrains.com/issue/KT-20994) JS extension property in interface problem
- [`KT-21004`](https://youtrack.jetbrains.com/issue/KT-21004) JS: don't use short-circuit operators when translating Boolean.and/or(Boolean)
- [`KT-21026`](https://youtrack.jetbrains.com/issue/KT-21026) JS: wrong code generated for suspend fun that calls inline suspend fun as a tail call.
- [`KT-21041`](https://youtrack.jetbrains.com/issue/KT-21041) 'TypeError: ... is not a function' for lambda with closure passed as an argument to super type constructor
- [`KT-21043`](https://youtrack.jetbrains.com/issue/KT-21043) JS: inlining coroutine from other module sometimes causes incorrect code generated
- [`KT-21093`](https://youtrack.jetbrains.com/issue/KT-21093) Kotlin.JS doesnt escape ‘in’ identifier and conflicts with in keyword
- [`KT-21245`](https://youtrack.jetbrains.com/issue/KT-21245) JS: interface function with default parameter, overridden by other interface indirectly cannot be found at runtime
- [`KT-21307`](https://youtrack.jetbrains.com/issue/KT-21307) JS DCE does not remap paths to sources
- [`KT-21309`](https://youtrack.jetbrains.com/issue/KT-21309) JS: incorrect source map generated for inline lambda when it's last expression is a statement-like expression (e.g. when or try/catch)
- [`KT-21317`](https://youtrack.jetbrains.com/issue/KT-21317) JS: safe call to suspend function returning Unit causes incorrect 
- [`KT-21421`](https://youtrack.jetbrains.com/issue/KT-21421) JS: accesors of overridden char properties with backing fields aren't boxed
- [`KT-21468`](https://youtrack.jetbrains.com/issue/KT-21468) JS: don't use enum entry's name for when over external enums
- [`KT-21850`](https://youtrack.jetbrains.com/issue/KT-21850) JS: support nested tests
### Language design

- [`KT-10532`](https://youtrack.jetbrains.com/issue/KT-10532) ISE by ThrowingLexicalScope at compile time with specific override chain

### Libraries

- [`KT-20864`](https://youtrack.jetbrains.com/issue/KT-20864) Provide `ReadOnly` and `Mutable` annotations to control java collection mutability in kotlin
- [`KT-18789`](https://youtrack.jetbrains.com/issue/KT-18789) Delegating val to out-projected `MutableMap` resulted in NPE due to cast to `Nothing`
- [`KT-21828`](https://youtrack.jetbrains.com/issue/KT-21828) JS: The List produced by the `IntArray.asList` function caused weird results
- [`KT-21868`](https://youtrack.jetbrains.com/issue/KT-21868) Eliminate potential data race in `SafePublicationLazyImpl`
- [`KT-21918`](https://youtrack.jetbrains.com/issue/KT-21918) Make `toTypedArray()` implementation more efficient and thread-safe
- [`KT-22003`](https://youtrack.jetbrains.com/issue/KT-22003) JS: Replace `Regex` constructor-like functions with secondary constructors
- JS: `Volatile` and `Synchornized` annotations are moved to `kotlin.jvm` package with the migration type aliases provided
- [`KT-16348`](https://youtrack.jetbrains.com/issue/KT-16348) Provide `String.toBoolean()` conversion in JS and common platforms
- Add missing declarations to kotlin-stdlib-common, those that are already supported in both platforms
  - [`KT-21191`](https://youtrack.jetbrains.com/issue/KT-21191) Add missing exception constructors to common and JS declarations
  - [`KT-21861`](https://youtrack.jetbrains.com/issue/KT-21861) Provide `NumberFormatException` in common projects and make it inherit `IllegalArgumentException` in all platforms
  - Add missing `pattern` and `options` properties to common `Regex`
- [`KT-20968`](https://youtrack.jetbrains.com/issue/KT-20968) Improve docs for String.format and String.Companion.format

### Reflection

- [`KT-20875`](https://youtrack.jetbrains.com/issue/KT-20875) Support Void.TYPE as underlying Class object for KClass
- [`KT-21453`](https://youtrack.jetbrains.com/issue/KT-21453) NPE in TypeSignatureMappingKt#computeInternalName

### Tools

- [`KT-20298`](https://youtrack.jetbrains.com/issue/KT-20298) Lint warning when using @Parcelize with delegated properties
- [`KT-20299`](https://youtrack.jetbrains.com/issue/KT-20299) Android non-ASCII TextView Id Unresolved Reference Bug
- [`KT-20717`](https://youtrack.jetbrains.com/issue/KT-20717) @Parcelize Creator.newArray method is generated incorrectly
- [`KT-20751`](https://youtrack.jetbrains.com/issue/KT-20751) kotlin-spring compiler plugin does not open @Validated classes
- [`KT-21171`](https://youtrack.jetbrains.com/issue/KT-21171) _$_findViewCache and _$_findCachedViewById are created in Activity subclass without Kotlin Android Extensions.
- [`KT-21628`](https://youtrack.jetbrains.com/issue/KT-21628) Can't find referenced class kotlin.internal.annotations.AvoidUninitializedObjectCopyingCheck
- [`KT-21777`](https://youtrack.jetbrains.com/issue/KT-21777) RMI "Connection refused" errors with daemon
- [`KT-21992`](https://youtrack.jetbrains.com/issue/KT-21992) @Transient warning for lazy property

### Tools. Gradle

- [`KT-20892`](https://youtrack.jetbrains.com/issue/KT-20892) Support module name option in K2MetadataCompilerArguments
- [`KT-17621`](https://youtrack.jetbrains.com/issue/KT-17621) Incremental compilation is very slow when Java file is modified
- [`KT-14125`](https://youtrack.jetbrains.com/issue/KT-14125) Android-extensions don't track xml changes well
- [`KT-20233`](https://youtrack.jetbrains.com/issue/KT-20233) Kapt: using compiler in-process w/ gradle leads to classloader conflict
- [`KT-21009`](https://youtrack.jetbrains.com/issue/KT-21009) Running Gradle build with `clean` prevents `KotlinCompile` tasks from loading from cache
- [`KT-21596`](https://youtrack.jetbrains.com/issue/KT-21596) Improve Kapt Gradle Plugin to be more friendly for Kotlin-DSL
- [`KT-15753`](https://youtrack.jetbrains.com/issue/KT-15753) Support cacheable tasks
- [`KT-17656`](https://youtrack.jetbrains.com/issue/KT-17656) Kotlin and Kotlin Android plugin not using available build caches
- [`KT-20017`](https://youtrack.jetbrains.com/issue/KT-20017) Support local (non-relocatable) Gradle build cache
- [`KT-20598`](https://youtrack.jetbrains.com/issue/KT-20598) Missing input annotations on AbstractKotlinCompileTool
- [`KT-20604`](https://youtrack.jetbrains.com/issue/KT-20604) Kotlin plugin breaks relocatability and compile avoidance for Java compile tasks
- [`KT-21203`](https://youtrack.jetbrains.com/issue/KT-21203) Kotlin gradle plugin does not create proper Ivy metadata for dependencies
- [`KT-21261`](https://youtrack.jetbrains.com/issue/KT-21261) Gradle plugin 1.1.60 creates "build-history.bin" outside project.buildDir
- [`KT-21805`](https://youtrack.jetbrains.com/issue/KT-21805) Gradle plugin does not work with JDK 1.7 (KaptGradleModel)
- [`KT-21806`](https://youtrack.jetbrains.com/issue/KT-21806) Gradle Plugin: Using automatic dependency versions with 'maven-publish' plugin does not include dependency version in generated publication POMs
### Tools. Incremental Compile

- [`KT-20840`](https://youtrack.jetbrains.com/issue/KT-20840) Multiplatform IC fails if expected or actual file is modified separately
- [`KT-21622`](https://youtrack.jetbrains.com/issue/KT-21622) Make IC work more accurately with changes of Android layouts xml files
- [`KT-21699`](https://youtrack.jetbrains.com/issue/KT-21699) JS IC produces different source maps when enum usage is compiled separately
- [`KT-20633`](https://youtrack.jetbrains.com/issue/KT-20633) Class is not recompiled

### Tools. J2K

- [`KT-21502`](https://youtrack.jetbrains.com/issue/KT-21502) Inspection to convert map.put(k, v) into map[k] = v
- [`KT-19390`](https://youtrack.jetbrains.com/issue/KT-19390) Character and string concatenation in Java is converted to code with multiple type errors in Kotlin
- [`KT-19943`](https://youtrack.jetbrains.com/issue/KT-19943) Redundant 'toInt' after converting explicit Integer#intValue call

### Tools. JPS

- [`KT-21574`](https://youtrack.jetbrains.com/issue/KT-21574) JPS build: API version in project settings is ignored
- [`KT-21841`](https://youtrack.jetbrains.com/issue/KT-21841) JPS throws exception creating temporary file for module
- [`KT-21962`](https://youtrack.jetbrains.com/issue/KT-21962) Source file dependencies (lookups) are not tracked in JPS when Kotlin daemon is used

### Tools. Maven

- [`KT-20816`](https://youtrack.jetbrains.com/issue/KT-20816) Repeated Maven Compiles With Kapt Fail

### Tools. REPL

- [`KT-17561`](https://youtrack.jetbrains.com/issue/KT-17561) Embedding kotlin-script-utils may cause version conflicts e.g. with guava
- [`KT-17921`](https://youtrack.jetbrains.com/issue/KT-17921) The JSR 223 scripting engine fails to eval anything after encountering an unresolved reference
- [`KT-21075`](https://youtrack.jetbrains.com/issue/KT-21075) KotlinJsr223JvmLocalScriptEngineFactory does not well with kotlin-compiler-embeddable
- [`KT-21141`](https://youtrack.jetbrains.com/issue/KT-21141) Kotlin script: KotlinJsr223JvmLocalScriptEngine.state.history.reset() seems not clearing the compiler cache

### Tools. kapt

#### Fixes

- [`KT-18791`](https://youtrack.jetbrains.com/issue/KT-18791) Kapt: Constants from R class should not be inlined
- [`KT-19203`](https://youtrack.jetbrains.com/issue/KT-19203) Kapt3 generator doesn't seem to print log level lower to Mandatory Warning
- [`KT-19402`](https://youtrack.jetbrains.com/issue/KT-19402) `kapt.correctErrorTypes` makes typealias not work.
- [`KT-19505`](https://youtrack.jetbrains.com/issue/KT-19505) Kapt doesn't always stub classes about to be generated.
- [`KT-19518`](https://youtrack.jetbrains.com/issue/KT-19518) Kapt: Support 'correctErrorTypes' option in annotations
- [`KT-20257`](https://youtrack.jetbrains.com/issue/KT-20257) Kapt is incompatible with compiler plugins
- [`KT-20749`](https://youtrack.jetbrains.com/issue/KT-20749) Kapt: Support Java 9
- [`KT-21144`](https://youtrack.jetbrains.com/issue/KT-21144) Kapt: Compilation error with maven plugin (Java 9 compatibility)
- [`KT-21205`](https://youtrack.jetbrains.com/issue/KT-21205) KDoc unavailable via javax.lang.model.util.Elements#getDocComment(Element e)
- [`KT-21262`](https://youtrack.jetbrains.com/issue/KT-21262) Kapt: Remove artificial KaptError exception on errors from annotation processor
- [`KT-21264`](https://youtrack.jetbrains.com/issue/KT-21264) Kapt: -Xmaxerrs javac option is not propagated properly
- [`KT-21358`](https://youtrack.jetbrains.com/issue/KT-21358) Kapt: Support import directive with aliases (correctErrorTypes)
- [`KT-21359`](https://youtrack.jetbrains.com/issue/KT-21359) Kapt: Filter out non-package imports whenever possible (correctErrorTypes)
- [`KT-21425`](https://youtrack.jetbrains.com/issue/KT-21425) kapt warning when assembling unit tests
- [`KT-21433`](https://youtrack.jetbrains.com/issue/KT-21433) Annotations on enum constants are not kept on the generated stub
- [`KT-21483`](https://youtrack.jetbrains.com/issue/KT-21483) Kapt: Loading resources doesn't work without restarting the gradle daemon
- [`KT-21542`](https://youtrack.jetbrains.com/issue/KT-21542) Kapt: Report additional info about time spent in each annotation processor
- [`KT-21565`](https://youtrack.jetbrains.com/issue/KT-21565) Kapt, Maven: Support passing arguments for annotation processors
- [`KT-21566`](https://youtrack.jetbrains.com/issue/KT-21566) Kapt, Maven: Support passing Javac options
- [`KT-21729`](https://youtrack.jetbrains.com/issue/KT-21729) Error message says "androidProcessor" should be "annotationProcessor"
- [`KT-21936`](https://youtrack.jetbrains.com/issue/KT-21936) Kapt 1.2.20-eap:  cannot find symbol @KaptSignature
- [`KT-21735`](https://youtrack.jetbrains.com/issue/KT-21735) Kapt cache not cleared
- [`KT-22056`](https://youtrack.jetbrains.com/issue/KT-22056) Applying Kapt plugin causes RuntimeException on Gradle import: "Kapt importer for generated source roots failed, source root name: debug" at KaptProjectResolverExtension.populateAndroidModuleModelIfNeeded()
- [`KT-22189`](https://youtrack.jetbrains.com/issue/KT-22189) ISE from com.sun.tools.javac.util.Context.checkState when switching from 1.2.10 to 1.2.20-eap-33
## Previous releases

This release also includes the fixes and improvements from the previous
[`1.2.10`](https://github.com/JetBrains/kotlin/blob/1.2.0/ChangeLog.md) release.