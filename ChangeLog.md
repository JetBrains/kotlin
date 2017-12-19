# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.2.20

### Android

- [`KT-20085`](https://youtrack.jetbrains.com/issue/KT-20085) Android Extensions: ClassCastException after changing type of view in layout XML
- [`KT-20235`](https://youtrack.jetbrains.com/issue/KT-20235) Error, can't use plugin kotlin-android-extensions
- [`KT-20269`](https://youtrack.jetbrains.com/issue/KT-20269) Mark 'kapt.kotlin.generated' as a source root automatically in Android projects
- [`KT-20545`](https://youtrack.jetbrains.com/issue/KT-20545) Parcelable: Migrate to canonical NEW-DUP-INVOKESPECIAL form
- [`KT-20742`](https://youtrack.jetbrains.com/issue/KT-20742) @Serializable and @Parcelize do not work together
- [`KT-20928`](https://youtrack.jetbrains.com/issue/KT-20928) @Parcelize. Verify Error for Android Api 19

### Binary Metadata

- [`KT-11586`](https://youtrack.jetbrains.com/issue/KT-11586) Class literal annotation arguments are not yet supported in AnnotationSerializer

### Compiler

#### New Features

- [`KT-17944`](https://youtrack.jetbrains.com/issue/KT-17944) Allow 'expect' final member be implemented by 'actual' open member
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

### IDE

#### New Features

- [`KT-13378`](https://youtrack.jetbrains.com/issue/KT-13378) Provide ability to configure highlighting for !! in expressions and ? in types
- [`KT-17928`](https://youtrack.jetbrains.com/issue/KT-17928) Support code folding for primary constructors
- [`KT-20591`](https://youtrack.jetbrains.com/issue/KT-20591) Show @StringRes/@IntegerRes annotations in parameter info
- [`KT-20952`](https://youtrack.jetbrains.com/issue/KT-20952) "Navigate | Related symbol" should support expect/actual navigation
- [`KT-21229`](https://youtrack.jetbrains.com/issue/KT-21229) Make it possible to explicitly select "latest" language/API version
#### Performance Improvements

- [`KT-17367`](https://youtrack.jetbrains.com/issue/KT-17367) Rebuild requested for index KotlinJavaScriptMetaFileIndex 
- [`KT-21557`](https://youtrack.jetbrains.com/issue/KT-21557) IntelliJ 2017.3 Gradle Performance Regression
- [`KT-21632`](https://youtrack.jetbrains.com/issue/KT-21632) Freezing on typing
- [`KT-21701`](https://youtrack.jetbrains.com/issue/KT-21701) IDEA 2017.3 high CPU usage
#### Fixes

- [`KT-15254`](https://youtrack.jetbrains.com/issue/KT-15254) Please use Platform icons for "Run" icon in gutter
- [`KT-17254`](https://youtrack.jetbrains.com/issue/KT-17254) Remove obsolete unfold-icons in structure view 
- [`KT-17838`](https://youtrack.jetbrains.com/issue/KT-17838) Can't report exceptions from the Kotlin plugin 1.1.4-dev-119 in IDEA #IU-171.4424.37
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
- [`KT-19823`](https://youtrack.jetbrains.com/issue/KT-19823) Kotlin Gradle project import into IntelliJ: import kapt generated classes into classpath
- [`KT-19824`](https://youtrack.jetbrains.com/issue/KT-19824) Please provide a separate icon for a common library
- [`KT-20096`](https://youtrack.jetbrains.com/issue/KT-20096) Kotlin Gradle script: SOE after beginning of Pair definition before some script section
- [`KT-20329`](https://youtrack.jetbrains.com/issue/KT-20329) multiplatform: gutter "Is subclassed by" should show expect subclass from the common module
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
- [`KT-21200`](https://youtrack.jetbrains.com/issue/KT-21200) The Structure-view for Kotlin files could be improved
- [`KT-21318`](https://youtrack.jetbrains.com/issue/KT-21318) Highlighting of function exit points does not work if the function is a getter for property
- [`KT-21363`](https://youtrack.jetbrains.com/issue/KT-21363) IDE: kotlinc.xml with KotlinCommonCompilerArguments: build fails with UOE: "Operation is not supported for read-only collection" at EmptyList.clear()
- [`KT-21409`](https://youtrack.jetbrains.com/issue/KT-21409) UAST: Super-call arguments are not modeled/visited
- [`KT-21418`](https://youtrack.jetbrains.com/issue/KT-21418) Gradle based project in IDEA 181: Kotlin facets are not created
- [`KT-21441`](https://youtrack.jetbrains.com/issue/KT-21441) Folding multiline strings adds a space at the start if there is not one.
- [`KT-21546`](https://youtrack.jetbrains.com/issue/KT-21546) java.lang.IllegalArgumentException: Unexpected container fatal IDE error
- [`KT-21575`](https://youtrack.jetbrains.com/issue/KT-21575) Secondary constructor call body is missing
- [`KT-21733`](https://youtrack.jetbrains.com/issue/KT-21733) Structure view is not updated
- [`KT-21745`](https://youtrack.jetbrains.com/issue/KT-21745) Warning and quickfix about kotlin-stdlib-jre7/8 -> kotlin-stdlib-jdk7/8 in Maven
- [`KT-21746`](https://youtrack.jetbrains.com/issue/KT-21746) Warning and quickfix about kotlin-stdlib-jre7/8 -> kotlin-stdlib-jdk7/8 in Gradle
- [`KT-21756`](https://youtrack.jetbrains.com/issue/KT-21756) Find Usages for "type" in ts2kt provokes exception
- [`KT-21770`](https://youtrack.jetbrains.com/issue/KT-21770) Pasting $this into an interpolated string shouldn't escape $
- [`KT-21852`](https://youtrack.jetbrains.com/issue/KT-21852) Custom API version is lost when settings are reopen after restarting IDE

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
- [`KT-13702`](https://youtrack.jetbrains.com/issue/KT-13702) Issue a warning when equals is called recursively within itself
- [`KT-18449`](https://youtrack.jetbrains.com/issue/KT-18449) multiplatform project: provide a quick fix "Implement methods" for a impl class
- [`KT-18828`](https://youtrack.jetbrains.com/issue/KT-18828) Provide an intention action to move a companion object member to top level
- [`KT-19103`](https://youtrack.jetbrains.com/issue/KT-19103) Inspection to remove unnecessary suspend modifier
- [`KT-20484`](https://youtrack.jetbrains.com/issue/KT-20484) Add quick fix to add required target to annotation used on a type
- [`KT-20492`](https://youtrack.jetbrains.com/issue/KT-20492) Offer "Simplify" intention for 'when' expression where only one branch is known to be true
- [`KT-20615`](https://youtrack.jetbrains.com/issue/KT-20615) Inspection to detect usages of values incorrectly marked by Kotlin as const from Java code
- [`KT-20631`](https://youtrack.jetbrains.com/issue/KT-20631) Inspection to detect use of Unit as a standalone expression
- [`KT-20644`](https://youtrack.jetbrains.com/issue/KT-20644) Warning for missing const paired with val modifier for primitives and strings
- [`KT-20714`](https://youtrack.jetbrains.com/issue/KT-20714) Inspection for self-assigment of properties
- [`KT-21023`](https://youtrack.jetbrains.com/issue/KT-21023) Inspection to highlight variables / functions with implicit `Nothing?` type
- [`KT-21560`](https://youtrack.jetbrains.com/issue/KT-21560) Inspection to sort modifiers
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
- [`KT-21726`](https://youtrack.jetbrains.com/issue/KT-21726) "arrayOf can be replaced with literal" inspection quick fix produces incompilable result in presence of spread operator
- [`KT-21727`](https://youtrack.jetbrains.com/issue/KT-21727) "Redundant spread operator" inspection does not report array literal

### IDE. Refactorings

#### New Features

- [`KT-20095`](https://youtrack.jetbrains.com/issue/KT-20095) Nice to have: Allow conversion of selected companion methods to methods with @JvmStatic
#### Fixes

- [`KT-15840`](https://youtrack.jetbrains.com/issue/KT-15840) Introduce type alias: don't change not-nullable type with nullable typealias
- [`KT-18594`](https://youtrack.jetbrains.com/issue/KT-18594) Refactor / Extract (Functional) Parameter are available for annotation arguments, but fail with AE: "Body element is not found"
- [`KT-20146`](https://youtrack.jetbrains.com/issue/KT-20146) IAE “parameter 'name' of NameUtil.splitNameIntoWords must not be null” at renaming class
- [`KT-20335`](https://youtrack.jetbrains.com/issue/KT-20335) Refactor → Extract Type Parameter: “AWT events are not allowed inside write action” after processing duplicates
- [`KT-20402`](https://youtrack.jetbrains.com/issue/KT-20402) Throwable “PsiElement(IDENTIFIER) by KotlinInplaceParameterIntroducer” on calling Refactor → Extract Parameter for default values
- [`KT-20403`](https://youtrack.jetbrains.com/issue/KT-20403) AE “Body element is not found” on calling Refactor → Extract Parameter for default values in constructor of class without body
- [`KT-20790`](https://youtrack.jetbrains.com/issue/KT-20790) Refactoring extension function/property overagressive
- [`KT-20766`](https://youtrack.jetbrains.com/issue/KT-20766) Typealias end-of-line is removed when moving function and typealias to new file
- [`KT-21071`](https://youtrack.jetbrains.com/issue/KT-21071) Cannot invoke move refactoring on a typealias
- [`KT-21162`](https://youtrack.jetbrains.com/issue/KT-21162) Adding parameters to kotlin data class leads to compilation error
- [`KT-21334`](https://youtrack.jetbrains.com/issue/KT-21334) Extract variable doesn't take into account the receiver of a bound callable reference
- [`KT-21371`](https://youtrack.jetbrains.com/issue/KT-21371) Rename refactoring sometimes erases identifier being renamed when popping up name proposals
- [`KT-21530`](https://youtrack.jetbrains.com/issue/KT-21530) KNPE in introduce variable

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

### Language design

- [`KT-10532`](https://youtrack.jetbrains.com/issue/KT-10532) ISE by ThrowingLexicalScope at compile time with specific override chain

### Libraries

- [`KT-18789`](https://youtrack.jetbrains.com/issue/KT-18789) Delegating val to out-projected MutableMap results in cast to Nothing
- [`KT-18869`](https://youtrack.jetbrains.com/issue/KT-18869) Generating intrinsified 'for-in-progression-with-step' loop requires exposing getProgressionLastElement functions (currently internal)
- [`KT-20968`](https://youtrack.jetbrains.com/issue/KT-20968) Improve docs for String.format and String.Companion.format
- [`KT-21828`](https://youtrack.jetbrains.com/issue/KT-21828) JS: The List produced by the IntArray.asList function causes weird results

### Reflection

- [`KT-20875`](https://youtrack.jetbrains.com/issue/KT-20875) Support Void.TYPE as underlying Class object for KClass
- [`KT-21453`](https://youtrack.jetbrains.com/issue/KT-21453) NPE in TypeSignatureMappingKt#computeInternalName

### Tools

- [`KT-20298`](https://youtrack.jetbrains.com/issue/KT-20298) Lint warning when using @Parcelize with delegated properties
- [`KT-20299`](https://youtrack.jetbrains.com/issue/KT-20299) Android non-ASCII TextView Id Unresolved Reference Bug
- [`KT-20717`](https://youtrack.jetbrains.com/issue/KT-20717) @Parcelize Creator.newArray method is generated incorrectly
- [`KT-20751`](https://youtrack.jetbrains.com/issue/KT-20751) kotlin-spring compiler plugin does not open @Validated classes
- [`KT-21171`](https://youtrack.jetbrains.com/issue/KT-21171) _$_findViewCache and _$_findCachedViewById are created in Activity subclass without Kotlin Android Extensions.

### Tools. Gradle

- [`KT-20892`](https://youtrack.jetbrains.com/issue/KT-20892) Support module name option in K2MetadataCompilerArguments
- [`KT-17621`](https://youtrack.jetbrains.com/issue/KT-17621) Incremental compilation is very slow when Java file is modified
- [`KT-14125`](https://youtrack.jetbrains.com/issue/KT-14125) Android-extensions don't track xml changes well
- [`KT-20233`](https://youtrack.jetbrains.com/issue/KT-20233) Kapt: using compiler in-process w/ gradle leads to classloader conflict
- [`KT-21009`](https://youtrack.jetbrains.com/issue/KT-21009) Running Gradle build with `clean` prevents `KotlinCompile` tasks from loading from cache

### Tools. Incremental Compile

- [`KT-21622`](https://youtrack.jetbrains.com/issue/KT-21622) Make IC work more accurately with changes of Android layouts xml files
- [`KT-21699`](https://youtrack.jetbrains.com/issue/KT-21699) JS IC produces different source maps when enum usage is compiled separately

### Tools. J2K

- [`KT-21502`](https://youtrack.jetbrains.com/issue/KT-21502) Inspection to convert map.put(k, v) into map[k] = v
- [`KT-19390`](https://youtrack.jetbrains.com/issue/KT-19390) Character and string concatenation in Java is converted to code with multiple type errors in Kotlin
- [`KT-19943`](https://youtrack.jetbrains.com/issue/KT-19943) Redundant 'toInt' after converting explicit Integer#intValue call

### Tools. JPS

- [`KT-21574`](https://youtrack.jetbrains.com/issue/KT-21574) JPS build: API version in project settings is ignored
- [`KT-21841`](https://youtrack.jetbrains.com/issue/KT-21841) JPS throws exception creating temporary file for module

### Tools. Maven

- [`KT-20816`](https://youtrack.jetbrains.com/issue/KT-20816) Repeated Maven Compiles With Kapt Fail

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
