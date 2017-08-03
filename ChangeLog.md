# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.1.4-EAP-69

### Android

- [`KT-16934`](https://youtrack.jetbrains.com/issue/KT-16934) Android Extensions fails to compile when importing synthetic properties for layouts in other modules
- [`KT-18545`](https://youtrack.jetbrains.com/issue/KT-18545) Accessing to synthetic properties on smart casted Android components crashed compiler

### IDE

- [`KT-19303`](https://youtrack.jetbrains.com/issue/KT-19303) Project language version settings are used to analyze libraries, disabling module-specific analysis flags like -Xjsr305-annotations

### IDE. Debugger

- [`KT-14845`](https://youtrack.jetbrains.com/issue/KT-14845) Evaluate expression freezes debugger while evaluating filter, for time proportional to number of elements in collection.
- [`KT-19403`](https://youtrack.jetbrains.com/issue/KT-19403) 30s complete hangs of application on breakpoints stop attempt

### IDE. Inspections and Intentions

- [`KT-18506`](https://youtrack.jetbrains.com/issue/KT-18506) Inspection on final Kotlin spring components is false positive
- [`KT-18160`](https://youtrack.jetbrains.com/issue/KT-18160) Circular autofix actions between redundant modality and non-final variable with allopen plugin
- [`KT-18194`](https://youtrack.jetbrains.com/issue/KT-18194) "Protected in final" inspection works incorrectly with all-open
- [`KT-18195`](https://youtrack.jetbrains.com/issue/KT-18195) "Redundant modality" is not reported with all-open
- [`KT-18197`](https://youtrack.jetbrains.com/issue/KT-18197) Redundant "make open" for abstract class member with all-open
- [`KT-19272`](https://youtrack.jetbrains.com/issue/KT-19272) Do not report "function can be private" on JUnit 3 test methods

### IDE. Refactorings

- [`KT-16180`](https://youtrack.jetbrains.com/issue/KT-16180) Opened decompiled editor blocks refactoring of involved element

### Tools

- [`KT-18245`](https://youtrack.jetbrains.com/issue/KT-18245) NoArg: IllegalAccessError on instantiating sealed class child via Java reflection
- [`KT-19047`](https://youtrack.jetbrains.com/issue/KT-19047) Private methods are final event if used with the all-open-plugin.

### Tools. Gradle

- [`KT-18262`](https://youtrack.jetbrains.com/issue/KT-18262) kotlin-spring should also open @SpringBootTest classes
- [`KT-18832`](https://youtrack.jetbrains.com/issue/KT-18832) Java version parsing error with Gradle Kotlin plugin + JDK 9

### Tools. Maven

- [`KT-18022`](https://youtrack.jetbrains.com/issue/KT-18022) kotlin maven plugin - adding dependencies overwrites arguments.pluginClassPath preventing kapt goal from running

### Tools. kapt

- [`KT-18682`](https://youtrack.jetbrains.com/issue/KT-18682) Kapt: Anonymous class types are not rendered properly in stubs
- [`KT-18758`](https://youtrack.jetbrains.com/issue/KT-18758) Kotlin 1.1.3  / Kapt fails with gradle 
- [`KT-18799`](https://youtrack.jetbrains.com/issue/KT-18799) Kapt3, IC: Kapt does not generate annotation value for constant values in documented types

## 1.1.4-EAP-54

### Android

- [`KT-10542`](https://youtrack.jetbrains.com/issue/KT-10542) Android Extensions: No cache for Views
- [`KT-18250`](https://youtrack.jetbrains.com/issue/KT-18250) Android Extensions: Allow to use SparseArray as a View cache
- [`KT-14086`](https://youtrack.jetbrains.com/issue/KT-14086) Android-extensions not generated using flavors dimension
- [`KT-17641`](https://youtrack.jetbrains.com/issue/KT-17641) Problem with Kotlin Android Extensions and Gradle syntax
- [`KT-18012`](https://youtrack.jetbrains.com/issue/KT-18012) Kotlin Android Extensions generates `@NotNull` properties for views present in a configuration and potentially missing in another

### Compiler

- [`KT-14323`](https://youtrack.jetbrains.com/issue/KT-14323) IntelliJ lockup when using Apache Spark UDF
- [`KT-14375`](https://youtrack.jetbrains.com/issue/KT-14375) Kotlin compiler failure with spark when creating a flexible type for scala.Function22
- [`KT-18983`](https://youtrack.jetbrains.com/issue/KT-18983) Coroutines: miscompiled suspend for loop (local variables are not spilled around suspension points)
- [`KT-19175`](https://youtrack.jetbrains.com/issue/KT-19175) Compiler generates different bytecode when classes are compiled separately or together
- [`KT-19246`](https://youtrack.jetbrains.com/issue/KT-19246) Using generic inline function inside inline extension function throws java.lang.VerifyError: Bad return type

### IDE

- [`KT-14929`](https://youtrack.jetbrains.com/issue/KT-14929) Deprecated ReplaceWith for type aliases
- [`KT-14606`](https://youtrack.jetbrains.com/issue/KT-14606) Code completion calculates decompiled text when building lookup elements for PSI from compiled classes
- [`KT-17835`](https://youtrack.jetbrains.com/issue/KT-17835) 10s hang on IDEA project open
- [`KT-14561`](https://youtrack.jetbrains.com/issue/KT-14561) Use regular indent for the primary constructor parameters
- [`KT-17956`](https://youtrack.jetbrains.com/issue/KT-17956) Type hints for properties that only consist of constructor calls don't add much value
- [`KT-18444`](https://youtrack.jetbrains.com/issue/KT-18444) Type hints don't work for destructuring declarations
- [`KT-18974`](https://youtrack.jetbrains.com/issue/KT-18974) Type hints shouldn't appear for negative literals
- [`KT-19210`](https://youtrack.jetbrains.com/issue/KT-19210) Command line flags like -Xload-jsr305-annotations have no effect in IDE

### IDE. Completion

- [`KT-19191`](https://youtrack.jetbrains.com/issue/KT-19191) Disable completion binding context caching by default 

### IDE. Inspections and Intentions

- [`KT-13886`](https://youtrack.jetbrains.com/issue/KT-13886) Unused variable intention should remove constant initializer
- [`KT-16046`](https://youtrack.jetbrains.com/issue/KT-16046) Globally unused typealias is not marked as such
- [`KT-18368`](https://youtrack.jetbrains.com/issue/KT-18368) "Cast expression x to Type" fails for expression inside argument list
- [`KT-18852`](https://youtrack.jetbrains.com/issue/KT-18852) "Lift return out of when" does not work for exhaustive when without else
- [`KT-18928`](https://youtrack.jetbrains.com/issue/KT-18928) In IDE, "Replace 'if' expression with safe access expression incorrectly replace expression when using property
- [`KT-19232`](https://youtrack.jetbrains.com/issue/KT-19232) Replace Math.min with coerceAtMost intention is broken

### IDE. Refactorings

- [`KT-19130`](https://youtrack.jetbrains.com/issue/KT-19130) Refactor / Inline val: "Show inline dialog for local variables" setting is ignored

### Libraries

- [`KT-18671`](https://youtrack.jetbrains.com/issue/KT-18671) Provide implementation for CoroutineContext.Element functions.

### Tools. Gradle

- [`KT-17197`](https://youtrack.jetbrains.com/issue/KT-17197) Gradle Kotlin plugin does not wire task dependencies correctly, causing compilation failures

### Tools. JPS

- [`KT-19155`](https://youtrack.jetbrains.com/issue/KT-19155) IllegalArgumentException: Unsupported kind: PACKAGE_LOCAL_VARIABLE_LIST in incremental compilation

### Tools. kapt

- [`KT-19178`](https://youtrack.jetbrains.com/issue/KT-19178) Kapt: Build dependencies from 'kapt' configuration should go into the 'kaptCompile' task dependencies
- [`KT-19179`](https://youtrack.jetbrains.com/issue/KT-19179) Kapt: Gradle silently skips 'kotlinKapt' task sometimes
- [`KT-19211`](https://youtrack.jetbrains.com/issue/KT-19211) Kapt3: Generated classes output is not synchronized with Java classes output in pure Java projects (Gradle 4+)

## 1.1.4-EAP-33

### Compiler

- [`KT-12551`](https://youtrack.jetbrains.com/issue/KT-12551) Report "unused expression" on unused bound double colon expressions
- [`KT-18698`](https://youtrack.jetbrains.com/issue/KT-18698) java.lang.IllegalStateException: resolveToInstruction: incorrect index -1 for label L12 in subroutine
- [`KT-18916`](https://youtrack.jetbrains.com/issue/KT-18916) Strange bytecode generated for 'null' passed as SAM adapter for Java interface

### IDE

#### New Features

- [`KT-11994`](https://youtrack.jetbrains.com/issue/KT-11994) Data flow analysis support for Kotlin in IntelliJ
- [`KT-14126`](https://youtrack.jetbrains.com/issue/KT-14126) Code style wrapping options for enum constants
- [`KT-14950`](https://youtrack.jetbrains.com/issue/KT-14950) Code Style: Wrapping and Braces / "Local variable annotations" setting could be supported
#### Performance Improvements

- [`KT-18921`](https://youtrack.jetbrains.com/issue/KT-18921) Configure library kind explicitly
#### Fixes

- [`KT-14083`](https://youtrack.jetbrains.com/issue/KT-14083) Formatting of where clasuses
- [`KT-16352`](https://youtrack.jetbrains.com/issue/KT-16352) Create from usage inserts extra space in first step
- [`KT-17394`](https://youtrack.jetbrains.com/issue/KT-17394) Core formatting is wrong for expression body properties
- [`KT-17771`](https://youtrack.jetbrains.com/issue/KT-17771) Kotlin IntelliJ plugin should resolve Gradle script classpath asynchronously
- [`KT-17818`](https://youtrack.jetbrains.com/issue/KT-17818) Formatting of long constructors is inconsistent with Kotlin code conventions
- [`KT-18186`](https://youtrack.jetbrains.com/issue/KT-18186) Create function from usage should infer expected return type
- [`KT-19054`](https://youtrack.jetbrains.com/issue/KT-19054) Lags in typing in string literal
- [`KT-19062`](https://youtrack.jetbrains.com/issue/KT-19062) Member navigation doesn't work in expression bodies of getters with inferred property type

### IDE. Completion

- [`KT-17074`](https://youtrack.jetbrains.com/issue/KT-17074) Incorrect autocomplete suggestions for contexts affected by @DslMarker

### IDE. Debugger

- [`KT-17120`](https://youtrack.jetbrains.com/issue/KT-17120) Evaluate expression: cannot find local variable
- [`KT-18949`](https://youtrack.jetbrains.com/issue/KT-18949) Can't stop on breakpoint after call to inline in Android Studio

### IDE. Inspections and Intentions

#### New Features

- [`KT-14799`](https://youtrack.jetbrains.com/issue/KT-14799) Add inspection to simplify successive null checks into safe-call and null check
- [`KT-15958`](https://youtrack.jetbrains.com/issue/KT-15958) Inspection to inline "unnecessary" variables
- [`KT-17919`](https://youtrack.jetbrains.com/issue/KT-17919) Add "Simplify if" intention/inspection
- [`KT-18540`](https://youtrack.jetbrains.com/issue/KT-18540) Add quickfix to create data class property from usage in destructuring declaration
- [`KT-18830`](https://youtrack.jetbrains.com/issue/KT-18830) "Lift return out of try"
#### Fixes

- [`KT-13870`](https://youtrack.jetbrains.com/issue/KT-13870) Wrong caption "Change to property access" for Quick Fix to convert class instantiation to object reference
- [`KT-15242`](https://youtrack.jetbrains.com/issue/KT-15242) Create type from usage should include constraints into base types 
- [`KT-17092`](https://youtrack.jetbrains.com/issue/KT-17092) Create function from usage works incorrectly with ::class expression
- [`KT-17353`](https://youtrack.jetbrains.com/issue/KT-17353) "Create type parameter from usage" should not be offered for unresolved annotations
- [`KT-17651`](https://youtrack.jetbrains.com/issue/KT-17651) Create property from usage should make lateinit var
- [`KT-18074`](https://youtrack.jetbrains.com/issue/KT-18074) Suggestion in Intention 'Specify return type explicitly' doesn't support generic type parameter
- [`KT-18722`](https://youtrack.jetbrains.com/issue/KT-18722) Correct "before" sample in description for intention Convert to enum class
- [`KT-18723`](https://youtrack.jetbrains.com/issue/KT-18723) Correct "after" sample for intention Convert to apply
- [`KT-18954`](https://youtrack.jetbrains.com/issue/KT-18954) Kotlin plugin updater activates in headless mode
- [`KT-18970`](https://youtrack.jetbrains.com/issue/KT-18970) Do not report "property can be private" on JvmField properties

### IDE. Refactorings

- [`KT-18738`](https://youtrack.jetbrains.com/issue/KT-18738) Misleading quick fix message for an 'open' modifier on an interface member

### Reflection

- [`KT-15222`](https://youtrack.jetbrains.com/issue/KT-15222) Support reflection for local delegated properties

### Tools. Gradle

- [`KT-18647`](https://youtrack.jetbrains.com/issue/KT-18647) Kotlin incremental compile cannot be disabled.


## 1.1.4-EAP-11

### Android

- [`KT-11048`](https://youtrack.jetbrains.com/issue/KT-11048) Android Extensions: cannot evaluate expression containing generated properties
- [`KT-11051`](https://youtrack.jetbrains.com/issue/KT-11051) Android Extensions: completion of generated properties is unclear for ambiguous ids
- [`KT-14912`](https://youtrack.jetbrains.com/issue/KT-14912) Lint: "Code contains STOPSHIP marker" ignores suppress annotation
- [`KT-15164`](https://youtrack.jetbrains.com/issue/KT-15164) Kotlin Lint: problems in delegate expression are not reported
- [`KT-17783`](https://youtrack.jetbrains.com/issue/KT-17783) Kotlin Lint: quick fixes to add inapplicable @RequiresApi and @SuppressLint make code incompilable
- [`KT-17786`](https://youtrack.jetbrains.com/issue/KT-17786) Kotlin Lint: "Surround with if()" quick fix is not suggested for single expression `get()`
- [`KT-17787`](https://youtrack.jetbrains.com/issue/KT-17787) Kotlin Lint: "Add @TargetApi" quick fix is not suggested for top level property accessor
- [`KT-17788`](https://youtrack.jetbrains.com/issue/KT-17788) Kotlin Lint: "Surround with if()" quick fix corrupts code in case of destructuring declaration
- [`KT-17890`](https://youtrack.jetbrains.com/issue/KT-17890) [kotlin-android-extensions] Renaming layout file does not rename import

### Compiler

#### New Features

- [`KT-10942`](https://youtrack.jetbrains.com/issue/KT-10942) Support meta-annotations from JSR 305 for nullability qualifiers
- [`KT-14187`](https://youtrack.jetbrains.com/issue/KT-14187) Redundant "is" check is not detected
- [`KT-16603`](https://youtrack.jetbrains.com/issue/KT-16603) Support `inline suspend` function
- [`KT-17585`](https://youtrack.jetbrains.com/issue/KT-17585) Generate state machine for named functions in their bodies
#### Performance Improvements

- [`KT-3098`](https://youtrack.jetbrains.com/issue/KT-3098) Generate efficient comparisons
- [`KT-6247`](https://youtrack.jetbrains.com/issue/KT-6247) Optimization for 'in' and '..'
- [`KT-7571`](https://youtrack.jetbrains.com/issue/KT-7571) Don't box Double instance to call hashCode on Java 8
- [`KT-9900`](https://youtrack.jetbrains.com/issue/KT-9900) Optimize range operations for 'until' extension from stdlib
- [`KT-11959`](https://youtrack.jetbrains.com/issue/KT-11959) Unnceessary boxing/unboxing due to Comparable.compareTo
- [`KT-12158`](https://youtrack.jetbrains.com/issue/KT-12158) Optimize away boxing when comparing nullable primitive type value to primitive value
- [`KT-13682`](https://youtrack.jetbrains.com/issue/KT-13682) Reuse StringBuilder for concatenation and string interpolation
- [`KT-15235`](https://youtrack.jetbrains.com/issue/KT-15235) Escaped characters in template strings are generating inefficient implementations
- [`KT-17280`](https://youtrack.jetbrains.com/issue/KT-17280) Inline constant expressions in string templates
- [`KT-17903`](https://youtrack.jetbrains.com/issue/KT-17903) Generate 'for-in-indices' as a precondition loop
- [`KT-18157`](https://youtrack.jetbrains.com/issue/KT-18157) Optimize out trivial INSTANCEOF checks
- [`KT-18162`](https://youtrack.jetbrains.com/issue/KT-18162) Do not check nullability assertions twice for effectively same value
- [`KT-18164`](https://youtrack.jetbrains.com/issue/KT-18164) Do not check nullability for values that have been already checked with !!
- [`KT-18478`](https://youtrack.jetbrains.com/issue/KT-18478) Unnecessary nullification of bound variables
- [`KT-18558`](https://youtrack.jetbrains.com/issue/KT-18558) Flatten nested string concatenation
- [`KT-18777`](https://youtrack.jetbrains.com/issue/KT-18777) Unnecessary boolean negation generated for 'if (expr !in range)'
#### Fixes

- [`KT-1809`](https://youtrack.jetbrains.com/issue/KT-1809) Confusing diagnostics when wrong number of type arguments are specified and there are several callee candiates
- [`KT-2007`](https://youtrack.jetbrains.com/issue/KT-2007) Improve diagnostics when + in not resolved on a pair of nullable ints
- [`KT-5066`](https://youtrack.jetbrains.com/issue/KT-5066) Bad diagnostic message for ABSTRACT_MEMBER_NOT_IMPLEMENTED for (companion) object
- [`KT-5511`](https://youtrack.jetbrains.com/issue/KT-5511) Inconsistent handling of inner enum
- [`KT-7773`](https://youtrack.jetbrains.com/issue/KT-7773) Disallow to explicitly extend Enum<E> class
- [`KT-7975`](https://youtrack.jetbrains.com/issue/KT-7975) Unclear error message when redundant type arguments supplied
- [`KT-8340`](https://youtrack.jetbrains.com/issue/KT-8340) vararg in a property setter must be an error
- [`KT-8612`](https://youtrack.jetbrains.com/issue/KT-8612) Incorrect error message for var extension property without getter or setter
- [`KT-8829`](https://youtrack.jetbrains.com/issue/KT-8829) Type parameter of a class is not resolved in the constructor parameter's default value
- [`KT-8845`](https://youtrack.jetbrains.com/issue/KT-8845) Bogus diagnostic on infix operation "in"
- [`KT-9282`](https://youtrack.jetbrains.com/issue/KT-9282) Improve diagnostic on overload resolution ambiguity when a nullable argument is passed to non-null parameter
- [`KT-10045`](https://youtrack.jetbrains.com/issue/KT-10045) Not specific enough compiler error message in case of trying to call overloaded private methods
- [`KT-10164`](https://youtrack.jetbrains.com/issue/KT-10164) Incorrect error message for external inline method
- [`KT-10248`](https://youtrack.jetbrains.com/issue/KT-10248) Smart casts: Misleading error on overloaded function call
- [`KT-10657`](https://youtrack.jetbrains.com/issue/KT-10657) Confusing diagnostic when trying to invoke value as a function
- [`KT-10839`](https://youtrack.jetbrains.com/issue/KT-10839) Weird diagnostics on callable reference of unresolved class
- [`KT-11119`](https://youtrack.jetbrains.com/issue/KT-11119) Confusing error message when overloaded method is called on nullable receiver
- [`KT-12408`](https://youtrack.jetbrains.com/issue/KT-12408) Generic information lost for override values
- [`KT-13749`](https://youtrack.jetbrains.com/issue/KT-13749) Error highlighting range for no 'override' modifier is bigger than needed
- [`KT-14598`](https://youtrack.jetbrains.com/issue/KT-14598) Do not report "member is final and cannot be overridden" when overriding something from final class
- [`KT-14633`](https://youtrack.jetbrains.com/issue/KT-14633) "If must have both main and else branches" diagnostic range is too high
- [`KT-14647`](https://youtrack.jetbrains.com/issue/KT-14647) Confusing error message "'@receiver:' annotations could be applied only to extension function or extension property declarations"
- [`KT-14927`](https://youtrack.jetbrains.com/issue/KT-14927) TCE in QualifiedExpressionResolver
- [`KT-15243`](https://youtrack.jetbrains.com/issue/KT-15243) Report deprecation on usages of type alias expanded to a deprecated class
- [`KT-15804`](https://youtrack.jetbrains.com/issue/KT-15804) Prohibit having duplicate parameter names in functional types
- [`KT-15810`](https://youtrack.jetbrains.com/issue/KT-15810) destructuring declarations don't work in scripts on the top level
- [`KT-15931`](https://youtrack.jetbrains.com/issue/KT-15931) IllegalStateException: ClassDescriptor of superType should not be null: T by a
- [`KT-16016`](https://youtrack.jetbrains.com/issue/KT-16016) Compiler failure with NO_EXPECTED_TYPE
- [`KT-16448`](https://youtrack.jetbrains.com/issue/KT-16448) Inline suspend functions with inlined suspend invocations are miscompiled (VerifyError, ClassNotFound)
- [`KT-16576`](https://youtrack.jetbrains.com/issue/KT-16576) Wrong code generated with skynet benchmark
- [`KT-17007`](https://youtrack.jetbrains.com/issue/KT-17007) Kotlin is not optimizing away unreachable code based on const vals
- [`KT-17188`](https://youtrack.jetbrains.com/issue/KT-17188) Do not propose to specify constructor invocation for classes without an accessible constructor
- [`KT-17611`](https://youtrack.jetbrains.com/issue/KT-17611) Unnecessary "Name shadowed" warning on parameter of local function or local class member
- [`KT-17692`](https://youtrack.jetbrains.com/issue/KT-17692) NPE in compiler when calling KClass.java on function result of type Unit
- [`KT-17820`](https://youtrack.jetbrains.com/issue/KT-17820) False "useless cast" when target type is flexible
- [`KT-17972`](https://youtrack.jetbrains.com/issue/KT-17972) Anonymous class generated from lambda captures its outer and tries to set nonexistent this$0 field.
- [`KT-18029`](https://youtrack.jetbrains.com/issue/KT-18029) typealias not working in .kts files
- [`KT-18085`](https://youtrack.jetbrains.com/issue/KT-18085) Compilation Error:Kotlin: [Internal Error] kotlin.TypeCastException: null cannot be cast to non-null type com.intellij.psi.PsiElement
- [`KT-18115`](https://youtrack.jetbrains.com/issue/KT-18115) Generic inherited classes in different packages with coroutine causes java.lang.VerifyError: Bad local variable type
- [`KT-18189`](https://youtrack.jetbrains.com/issue/KT-18189) Incorrect generic signature generated for implementation methods overriding special built-ins
- [`KT-18234`](https://youtrack.jetbrains.com/issue/KT-18234) Top-level variables in script aren't local variables
- [`KT-18413`](https://youtrack.jetbrains.com/issue/KT-18413) Strange compiler error - probably incremental compiler
- [`KT-18486`](https://youtrack.jetbrains.com/issue/KT-18486) Superfluos generation of suspend function state-machine because of inner suspension of different coroutine
- [`KT-18598`](https://youtrack.jetbrains.com/issue/KT-18598) Report error on access to declarations from non-exported packages and from inaccessible modules on Java 9
- [`KT-18702`](https://youtrack.jetbrains.com/issue/KT-18702) Proguard warning with Kotlin 1.2-M1
- [`KT-18728`](https://youtrack.jetbrains.com/issue/KT-18728) Integer method reference application fails with CompilationException: Back-end (JVM) Internal error
- [`KT-18845`](https://youtrack.jetbrains.com/issue/KT-18845) Exception on building gradle project with collection literals 

### IDE

#### New Features

- [`KT-2638`](https://youtrack.jetbrains.com/issue/KT-2638) Inline property (with accessors) refactoring
- [`KT-7107`](https://youtrack.jetbrains.com/issue/KT-7107) Rename refactoring for labels
- [`KT-9818`](https://youtrack.jetbrains.com/issue/KT-9818) Code style for method expression bodies
- [`KT-14965`](https://youtrack.jetbrains.com/issue/KT-14965) "Configure Kotlin in project" should support build.gradle.kts
- [`KT-15504`](https://youtrack.jetbrains.com/issue/KT-15504) Add code style options to limit number of blank lines 
- [`KT-16558`](https://youtrack.jetbrains.com/issue/KT-16558) Code Style: Add Options for "Spaces Before Parentheses"
- [`KT-18113`](https://youtrack.jetbrains.com/issue/KT-18113) Add new line options to code style for method parameters
- [`KT-18605`](https://youtrack.jetbrains.com/issue/KT-18605) Option to not use continuation indent in chained calls
- [`KT-18607`](https://youtrack.jetbrains.com/issue/KT-18607) Options to put blank lines between 'when' branches
#### Performance Improvements

- [`KT-17751`](https://youtrack.jetbrains.com/issue/KT-17751) Kotlin slows down java inspections big time
#### Fixes

- [`KT-6610`](https://youtrack.jetbrains.com/issue/KT-6610) Language injection doesn't work with String Interpolation
- [`KT-8893`](https://youtrack.jetbrains.com/issue/KT-8893) Quick documentation shows type for top-level object-type elements, but "no name provided" for local ones
- [`KT-9359`](https://youtrack.jetbrains.com/issue/KT-9359) "Accidental override" error message does not mention class (type) names
- [`KT-10736`](https://youtrack.jetbrains.com/issue/KT-10736) Highlighting usages doesn't work for synthetic properties created by the Android Extensions
- [`KT-11980`](https://youtrack.jetbrains.com/issue/KT-11980) Spring: Generate Constructor, Setter Dependency in XML for Kotlin class: IOE at LightElement.add()
- [`KT-12123`](https://youtrack.jetbrains.com/issue/KT-12123) Formatter: always indent after newline in variable initialization
- [`KT-12910`](https://youtrack.jetbrains.com/issue/KT-12910) spring: create init-method/destroy-method from usage results in IOE
- [`KT-13072`](https://youtrack.jetbrains.com/issue/KT-13072) Kotlin struggles to index JDK 9 classes
- [`KT-13099`](https://youtrack.jetbrains.com/issue/KT-13099) formatting in angle brackets ignored and not fixed
- [`KT-14271`](https://youtrack.jetbrains.com/issue/KT-14271) Value captured in closure doesn't always get highlighted
- [`KT-14974`](https://youtrack.jetbrains.com/issue/KT-14974) "Find Usages" hangs in ExpressionsOfTypeProcessor
- [`KT-15270`](https://youtrack.jetbrains.com/issue/KT-15270) Quickfix to migrate from @native***
- [`KT-16725`](https://youtrack.jetbrains.com/issue/KT-16725) Formatter does not fix spaces before square brackets
- [`KT-16999`](https://youtrack.jetbrains.com/issue/KT-16999) "Parameter info" shows duplicates on toString
- [`KT-17357`](https://youtrack.jetbrains.com/issue/KT-17357) BuiltIns for module build with project LV settings, not with facet module settings
- [`KT-17759`](https://youtrack.jetbrains.com/issue/KT-17759) Breakpoints not working in JS
- [`KT-17849`](https://youtrack.jetbrains.com/issue/KT-17849) Automatically insert trimMargin() or trimIndent() on enter in multi-line strings
- [`KT-17855`](https://youtrack.jetbrains.com/issue/KT-17855) Main function is shown as unused
- [`KT-17894`](https://youtrack.jetbrains.com/issue/KT-17894) String `trimIndent` support inserts wrong indent in some cases
- [`KT-17942`](https://youtrack.jetbrains.com/issue/KT-17942) Enter in multiline string with injection doesn't add a proper indent
- [`KT-18006`](https://youtrack.jetbrains.com/issue/KT-18006) Copying part of string literal with escape sequences converts this sequences to special characters
- [`KT-18030`](https://youtrack.jetbrains.com/issue/KT-18030) Parameters hints: `kotlin.arrayOf(elements)` should be on the blacklist by default
- [`KT-18059`](https://youtrack.jetbrains.com/issue/KT-18059) Kotlin Lint: False positive error "requires api level 24" for interface method with body
- [`KT-18149`](https://youtrack.jetbrains.com/issue/KT-18149) PIEAE "Element class CompositeElement of type REFERENCE_EXPRESSION (class KtNameReferenceExpressionElementType)" at PsiInvalidElementAccessException.createByNode()
- [`KT-18151`](https://youtrack.jetbrains.com/issue/KT-18151) Do not import jdkHome from Gradle/Maven model
- [`KT-18158`](https://youtrack.jetbrains.com/issue/KT-18158) Expand selection should select the comment after expression getter on the same line
- [`KT-18221`](https://youtrack.jetbrains.com/issue/KT-18221) AE at org.jetbrains.kotlin.analyzer.ResolverForProjectImpl.descriptorForModule
- [`KT-18269`](https://youtrack.jetbrains.com/issue/KT-18269) Find Usages fails to find operator-style usages of `invoke()` defined as extension
- [`KT-18298`](https://youtrack.jetbrains.com/issue/KT-18298) spring: strange menu at "Navige to the spring bean" gutter
- [`KT-18309`](https://youtrack.jetbrains.com/issue/KT-18309) Join lines breaks code
- [`KT-18373`](https://youtrack.jetbrains.com/issue/KT-18373) Facet: can't change target platform between JVM versions
- [`KT-18376`](https://youtrack.jetbrains.com/issue/KT-18376) Maven import fails with NPE at ArgumentUtils.convertArgumentsToStringList() if `jvmTarget` setting is absent
- [`KT-18418`](https://youtrack.jetbrains.com/issue/KT-18418) Generate equals and hashCode should be available for classes without properties
- [`KT-18429`](https://youtrack.jetbrains.com/issue/KT-18429) Android strings resources folding false positives
- [`KT-18475`](https://youtrack.jetbrains.com/issue/KT-18475) Gradle/IntelliJ sync can result in IntelliJ modules getting gradle artifacts added to the classpath, breaking compilation
- [`KT-18479`](https://youtrack.jetbrains.com/issue/KT-18479) Can't find usages of invoke operator with vararg parameter
- [`KT-18566`](https://youtrack.jetbrains.com/issue/KT-18566) Long find usages for operators when there are several operators for the same type
- [`KT-18596`](https://youtrack.jetbrains.com/issue/KT-18596) "Generate hashCode" produces poorly formatted code
- [`KT-18725`](https://youtrack.jetbrains.com/issue/KT-18725) Android: `kotlin-language` facet disappears on reopening the project

### IDE. Completion

- [`KT-8208`](https://youtrack.jetbrains.com/issue/KT-8208) Support static member completion with not-imported-yet classes
- [`KT-12104`](https://youtrack.jetbrains.com/issue/KT-12104) Smart completion does not work with "invoke" when receiver is expression
- [`KT-18443`](https://youtrack.jetbrains.com/issue/KT-18443) IntelliJ not handling default constructor argument from companion object well

### IDE. Debugger

- [`KT-18453`](https://youtrack.jetbrains.com/issue/KT-18453) Support 'Step over' and 'Force step over' action for suspended calls
- [`KT-18577`](https://youtrack.jetbrains.com/issue/KT-18577) Debug: Smart Step Into does not enter functions passed as variable or parameter: "Method invoke() has not been called"
- [`KT-18632`](https://youtrack.jetbrains.com/issue/KT-18632) Debug: Smart Step Into does not enter functions passed as variable or parameter when signature of lambda and parameter doesn't match

### IDE. Inspections and Intentions

#### New Features

- [`KT-12119`](https://youtrack.jetbrains.com/issue/KT-12119) Intention to replace .addAll() on a mutable collection with +=
- [`KT-13436`](https://youtrack.jetbrains.com/issue/KT-13436) Replace 'when' with return: handle case when all branches jump out (return Nothing)
- [`KT-13458`](https://youtrack.jetbrains.com/issue/KT-13458) Cascade "replace with return" for if/when expressions
- [`KT-13676`](https://youtrack.jetbrains.com/issue/KT-13676) Add better quickfix for 'let' and 'error  'only not null or asserted calls are allowed'
- [`KT-14648`](https://youtrack.jetbrains.com/issue/KT-14648) Add quickfix for @receiver annotation being applied to extension member instead of extension type
- [`KT-14900`](https://youtrack.jetbrains.com/issue/KT-14900) "Lift return out of when/if" should work with control flow expressions
- [`KT-15257`](https://youtrack.jetbrains.com/issue/KT-15257) JS: quickfix to migrate from @native to external
- [`KT-15368`](https://youtrack.jetbrains.com/issue/KT-15368) Add intention to convert Boolean? == true to ?: false and vice versa
- [`KT-15893`](https://youtrack.jetbrains.com/issue/KT-15893) "Array property in data class" inspection could have a quick fix to generate `equals()` and `hashcode()`
- [`KT-16063`](https://youtrack.jetbrains.com/issue/KT-16063) Inspection to suggest converting block body to expression body
- [`KT-17198`](https://youtrack.jetbrains.com/issue/KT-17198) Inspection to replace filter calls followed by functions with a predicate variant
- [`KT-17580`](https://youtrack.jetbrains.com/issue/KT-17580) Add remaning branches intention should be available for sealed classes
- [`KT-17583`](https://youtrack.jetbrains.com/issue/KT-17583) Support "Declaration access can be weaker"  inspection for kotlin properties
- [`KT-17815`](https://youtrack.jetbrains.com/issue/KT-17815) Quick-fix "Replace with safe call & elvis"
- [`KT-17842`](https://youtrack.jetbrains.com/issue/KT-17842) Add quick-fix for NO_CONSTRUCTOR error
- [`KT-17895`](https://youtrack.jetbrains.com/issue/KT-17895) Inspection to replace 'a .. b-1' with 'a until b'
- [`KT-17920`](https://youtrack.jetbrains.com/issue/KT-17920) Add intention/inspection removing redundant spread operator for arrayOf call
- [`KT-17970`](https://youtrack.jetbrains.com/issue/KT-17970) Intention actions to format parameter/argument list placing each on separate line
- [`KT-18236`](https://youtrack.jetbrains.com/issue/KT-18236) Add inspection for potentially wrongly placed unary operators
- [`KT-18274`](https://youtrack.jetbrains.com/issue/KT-18274) Add inspection to replace map+joinTo with joinTo(transform)
- [`KT-18386`](https://youtrack.jetbrains.com/issue/KT-18386) Inspection to detect safe calls of orEmpty()
- [`KT-18438`](https://youtrack.jetbrains.com/issue/KT-18438) Add inspection for empty ranges with start > endInclusive
- [`KT-18460`](https://youtrack.jetbrains.com/issue/KT-18460) Add intentions to apply De Morgan's laws to conditions
- [`KT-18516`](https://youtrack.jetbrains.com/issue/KT-18516) Add inspection to detect & remove redundant Unit
- [`KT-18517`](https://youtrack.jetbrains.com/issue/KT-18517) Provide "Remove explicit type" inspection for some obvious cases
- [`KT-18534`](https://youtrack.jetbrains.com/issue/KT-18534) Quick-fix to add empty brackets after primary constructor
- [`KT-18615`](https://youtrack.jetbrains.com/issue/KT-18615) Inspection to replace if with three or more options with when
- [`KT-18749`](https://youtrack.jetbrains.com/issue/KT-18749) Inspection for useless operations on collection with not-null elements
#### Fixes

- [`KT-11906`](https://youtrack.jetbrains.com/issue/KT-11906) Spring: "Create getter / setter" quick fixes cause IOE at LightElement.add()
- [`KT-12524`](https://youtrack.jetbrains.com/issue/KT-12524) Wrong "redundant semicolon" for semicolon inside an enum class before the companion object declaration
- [`KT-14092`](https://youtrack.jetbrains.com/issue/KT-14092) "Make <modifier>" intention inserts modifier between annotation and class keywords
- [`KT-14093`](https://youtrack.jetbrains.com/issue/KT-14093) "Make <modifier>" intention available only on modifier when declaration already have a visibility modifier
- [`KT-14643`](https://youtrack.jetbrains.com/issue/KT-14643) "Add non-null asserted call" quickfix should not be offered on literal null constants
- [`KT-16069`](https://youtrack.jetbrains.com/issue/KT-16069) "Simplify if statement" doesn't work in specific case
- [`KT-17026`](https://youtrack.jetbrains.com/issue/KT-17026) "Replace explicit parameter" should not be shown on destructuring declaration
- [`KT-17537`](https://youtrack.jetbrains.com/issue/KT-17537) Create from Usage should suggest Boolean return type if function is used in if condition
- [`KT-17623`](https://youtrack.jetbrains.com/issue/KT-17623) "Remove explicit type arguments" is too conservative sometimes
- [`KT-17726`](https://youtrack.jetbrains.com/issue/KT-17726) Nullability quick-fixes operate incorrectly with implicit nullable receiver
- [`KT-17740`](https://youtrack.jetbrains.com/issue/KT-17740) CME at MakeOverriddenMemberOpenFix.getText()
- [`KT-17823`](https://youtrack.jetbrains.com/issue/KT-17823) Intention "Make private" and friends should respect modifier order
- [`KT-17917`](https://youtrack.jetbrains.com/issue/KT-17917) Superfluos suggestion to add replaceWith for DeprecationLevel.HIDDEN
- [`KT-17954`](https://youtrack.jetbrains.com/issue/KT-17954) Setting error severity on "Kotlin | Function or property has platform type" does not show up as error in IDE
- [`KT-17996`](https://youtrack.jetbrains.com/issue/KT-17996) Android Studio Default Constructor Command Removes Custom Setter
- [`KT-18033`](https://youtrack.jetbrains.com/issue/KT-18033) Do not suggest to cast expression to non-nullable type when it's the same as !!
- [`KT-18035`](https://youtrack.jetbrains.com/issue/KT-18035) Quickfix for "CanBePrimaryConstructorProperty" does not work correctly with vararg constructor properties
- [`KT-18044`](https://youtrack.jetbrains.com/issue/KT-18044) "Move to class body" intention: better placement in the body
- [`KT-18120`](https://youtrack.jetbrains.com/issue/KT-18120) Recursive property accessor gives false positives
- [`KT-18148`](https://youtrack.jetbrains.com/issue/KT-18148) Incorrect, not working quickfix - final and can't be overridden
- [`KT-18253`](https://youtrack.jetbrains.com/issue/KT-18253) Wrong location of "Redundant 'toString()' call in string template" quickfix
- [`KT-18347`](https://youtrack.jetbrains.com/issue/KT-18347) Nullability quickfixes are not helpful when using invoke operator
- [`KT-18375`](https://youtrack.jetbrains.com/issue/KT-18375) Backticked function name is suggested to be renamed to the same name
- [`KT-18385`](https://youtrack.jetbrains.com/issue/KT-18385) Spring: Generate Dependency causes Throwable "AWT events are not allowed inside write action"
- [`KT-18407`](https://youtrack.jetbrains.com/issue/KT-18407) "Move property to constructor" action should not appear on properties declared in interfaces
- [`KT-18425`](https://youtrack.jetbrains.com/issue/KT-18425) Make <modifier> intention inserts modifier at wrong position for sealed class
- [`KT-18529`](https://youtrack.jetbrains.com/issue/KT-18529) Add '!!' quick fix applies to wrong expression on operation 'in'
- [`KT-18642`](https://youtrack.jetbrains.com/issue/KT-18642) Remove unused parameter intention transfers default value to another parameter
- [`KT-18683`](https://youtrack.jetbrains.com/issue/KT-18683) Wrong 'equals' is generated for Kotlin JS project
- [`KT-18709`](https://youtrack.jetbrains.com/issue/KT-18709) "Lift assignment out of if" changes semantics
- [`KT-18711`](https://youtrack.jetbrains.com/issue/KT-18711) "Lift return out of when" changes semantics for functional type
- [`KT-18717`](https://youtrack.jetbrains.com/issue/KT-18717) Report MemberVisibilityCanBePrivate on visibility modifier if present

### IDE. Refactorings

#### New Features

- [`KT-4379`](https://youtrack.jetbrains.com/issue/KT-4379) Support renaming import alias
- [`KT-8180`](https://youtrack.jetbrains.com/issue/KT-8180) Copy Class
- [`KT-17547`](https://youtrack.jetbrains.com/issue/KT-17547) Refactor / Move: Problems Detected / Conflicts in View: only referencing file is mentioned
#### Fixes

- [`KT-9054`](https://youtrack.jetbrains.com/issue/KT-9054) Copy / pasting a Kotlin file should bring up the Copy Class dialog
- [`KT-13437`](https://youtrack.jetbrains.com/issue/KT-13437) Change signature replaces return type with Unit when it's not requested
- [`KT-15859`](https://youtrack.jetbrains.com/issue/KT-15859) Renaming variables or functions with backticks removes the backticks
- [`KT-17062`](https://youtrack.jetbrains.com/issue/KT-17062) Field/property inline refactoring works incorrectly with Kotlin & Java usages
- [`KT-17128`](https://youtrack.jetbrains.com/issue/KT-17128) Refactor / Rename in the last position of label name throws Throwable "PsiElement(IDENTIFIER) by com.intellij.refactoring.rename.inplace.MemberInplaceRenamer" at InplaceRefactoring.buildTemplateAndStart()
- [`KT-17489`](https://youtrack.jetbrains.com/issue/KT-17489) Refactor / Inline Property: cannot inline val with the following plusAssign
- [`KT-17571`](https://youtrack.jetbrains.com/issue/KT-17571) Refactor / Move warns about using private/internal class from Java, but this is not related to the move
- [`KT-17622`](https://youtrack.jetbrains.com/issue/KT-17622) Refactor / Inline Function loses type arguments
- [`KT-18034`](https://youtrack.jetbrains.com/issue/KT-18034) Copy Class refactoring replaces all usages of the class with the new one!
- [`KT-18076`](https://youtrack.jetbrains.com/issue/KT-18076) Refactor / Rename on alias of Java class suggests to select between refactoring handlers
- [`KT-18096`](https://youtrack.jetbrains.com/issue/KT-18096) Refactor / Rename on import alias usage of a class member element tries to rename the element itself
- [`KT-18098`](https://youtrack.jetbrains.com/issue/KT-18098) Refactor / Copy can't generate proper import if original code uses import alias of java member
- [`KT-18135`](https://youtrack.jetbrains.com/issue/KT-18135) Refactor: no Problems Detected for Copy/Move source using platform type to another platform's module
- [`KT-18200`](https://youtrack.jetbrains.com/issue/KT-18200) Refactor / Copy is enabled for Java source selected with Kotlin file, but not for Java source selected with Kotlin class
- [`KT-18241`](https://youtrack.jetbrains.com/issue/KT-18241) Refactor / Copy (and Move) fails for chain of lambdas and invoke()'s with IllegalStateException: "No selector for PARENTHESIZED" at KtSimpleNameReference.changeQualifiedName()
- [`KT-18325`](https://youtrack.jetbrains.com/issue/KT-18325) Renaming a parameter name in one implementation silently rename it in all implementations
- [`KT-18390`](https://youtrack.jetbrains.com/issue/KT-18390) Refactor / Copy called for Java class opens only Copy File dialog
- [`KT-18699`](https://youtrack.jetbrains.com/issue/KT-18699) Refactor / Copy, Move loses necessary parentheses

### JavaScript

#### Performance Improvements

- [`KT-18331`](https://youtrack.jetbrains.com/issue/KT-18331) JS: compilation performance degrades fast when inlined nested labels are used
#### Fixes

- [`KT-4078`](https://youtrack.jetbrains.com/issue/KT-4078) JS sourcemaps should contain relative path. The relative base & prefix should be set from project/module preferences
- [`KT-8020`](https://youtrack.jetbrains.com/issue/KT-8020) JS: String? plus operator crashes on runtime
- [`KT-13919`](https://youtrack.jetbrains.com/issue/KT-13919) JS: Source map weirdness
- [`KT-15456`](https://youtrack.jetbrains.com/issue/KT-15456) JS: inlining doesn't work for array constructor with size and lambda
- [`KT-16984`](https://youtrack.jetbrains.com/issue/KT-16984) KotlinJS - 1 > 2 > false causes unhandled javascript exception
- [`KT-17285`](https://youtrack.jetbrains.com/issue/KT-17285) JS: wrong result when call function with default parameter overridden by delegation by function from another interface
- [`KT-17445`](https://youtrack.jetbrains.com/issue/KT-17445) JS: minifier for Kotlin JS apps
- [`KT-17476`](https://youtrack.jetbrains.com/issue/KT-17476) JS: Some symbols in identifiers compile, but are not legal
- [`KT-17871`](https://youtrack.jetbrains.com/issue/KT-17871) JS: spread vararg call doesn't work on functions imported with @JsModule
- [`KT-18027`](https://youtrack.jetbrains.com/issue/KT-18027) JS: Illegal symbols are possible in backticked labels, but cause crash in runtime and malformed js code
- [`KT-18032`](https://youtrack.jetbrains.com/issue/KT-18032) JS: Illegal symbols are possible in backticked package names, but cause crash in runtime and malformed js code
- [`KT-18169`](https://youtrack.jetbrains.com/issue/KT-18169) JS: reified generic backticked type name containing non-identifier symbols causes malformed JS and runtime crash
- [`KT-18187`](https://youtrack.jetbrains.com/issue/KT-18187) JS backend does not copy non-abstract method of interface to implementing class in some cases
- [`KT-18201`](https://youtrack.jetbrains.com/issue/KT-18201) JS backend generates wrong code for inline function which calls non-inline function from another module
- [`KT-18652`](https://youtrack.jetbrains.com/issue/KT-18652) JS: Objects from same package but from different libraries are incorrectly accessed

### Libraries

- [`KT-18526`](https://youtrack.jetbrains.com/issue/KT-18526) Small typo in documentation for kotlin-stdlib / kotlin.collections / retainAll
- [`KT-18624`](https://youtrack.jetbrains.com/issue/KT-18624) JS: Bad return type for Promise.all
- [`KT-18670`](https://youtrack.jetbrains.com/issue/KT-18670) Incorrect documentation of MutableMap.values

### Reflection

- [`KT-14094`](https://youtrack.jetbrains.com/issue/KT-14094) IllegalAccessException when try to get members annotated by private annotation with parameter
- [`KT-16399`](https://youtrack.jetbrains.com/issue/KT-16399) Embedded Tomcat fails to load Class-Path: kotlin-runtime.jar from kotlin-reflect-1.0.6.jar
- [`KT-16810`](https://youtrack.jetbrains.com/issue/KT-16810) Do not include incorrect ExternalOverridabilityCondition service file into kotlin-reflect.jar
- [`KT-18404`](https://youtrack.jetbrains.com/issue/KT-18404) “KotlinReflectionInternalError: This callable does not support a default call” when function or constructor has more than 32 parameters
- [`KT-18476`](https://youtrack.jetbrains.com/issue/KT-18476) KClass<*>.superclasses does not contain Any::class
- [`KT-18480`](https://youtrack.jetbrains.com/issue/KT-18480) Kotlin Reflection unable to call getter of protected read-only val with custom getter from parent class

### Tools

- [`KT-18062`](https://youtrack.jetbrains.com/issue/KT-18062) SamWithReceiver compiler plugin not used by IntelliJ for .kt files
- [`KT-18874`](https://youtrack.jetbrains.com/issue/KT-18874) Crash during compilation after switching to 1.1.3-release-IJ2017.2-2

### Tools. CLI

- [`KT-17297`](https://youtrack.jetbrains.com/issue/KT-17297) Report error when CLI compiler is not being run under Java 8+
- [`KT-18599`](https://youtrack.jetbrains.com/issue/KT-18599) Support -Xmodule-path and -Xadd-modules arguments for modular compilation on Java 9
- [`KT-18794`](https://youtrack.jetbrains.com/issue/KT-18794) kotlinc-jvm prints an irrelevant error message when a JVM Home directory does not exist
- [`KT-3045`](https://youtrack.jetbrains.com/issue/KT-3045) Report error instead of failing with exception on "kotlinc -script foo.kt"
- [`KT-18754`](https://youtrack.jetbrains.com/issue/KT-18754) Rename CLI argument "-module" to "-Xbuild-file"
- [`KT-18927`](https://youtrack.jetbrains.com/issue/KT-18927) run kotlin app crashes eclipse

### Tools. Gradle

- [`KT-10537`](https://youtrack.jetbrains.com/issue/KT-10537) Gradle plugin doesn't pick up changed project.buildDir 
- [`KT-17031`](https://youtrack.jetbrains.com/issue/KT-17031) JVM crash on in-process compilation in Gradle with debug
- [`KT-17618`](https://youtrack.jetbrains.com/issue/KT-17618) Pass freeCompilerArgs to compiler unchanged

### Tools. J2K

- [`KT-10762`](https://youtrack.jetbrains.com/issue/KT-10762) J2K removes empty lines from Doc-comments
- [`KT-13146`](https://youtrack.jetbrains.com/issue/KT-13146) J2K goes into infinite loop with anonymous inner class that references itself
- [`KT-15761`](https://youtrack.jetbrains.com/issue/KT-15761) Converting Java to Kotlin corrupts string which includes escaped backslash
- [`KT-16133`](https://youtrack.jetbrains.com/issue/KT-16133) Converting switch statement inserts dead code (possibly as a false positive for fall-through)
- [`KT-16142`](https://youtrack.jetbrains.com/issue/KT-16142) Kotlin Konverter produces empty line in Kdoc
- [`KT-18038`](https://youtrack.jetbrains.com/issue/KT-18038) Java to Kotlin converter messes up empty lines while converting from JavaDoc to KDoc
- [`KT-18051`](https://youtrack.jetbrains.com/issue/KT-18051) Doesn't work the auto-convert Java to Kotlin in Android Studio 3.0
- [`KT-18141`](https://youtrack.jetbrains.com/issue/KT-18141) J2K changes semantic when while does not have a body
- [`KT-18142`](https://youtrack.jetbrains.com/issue/KT-18142) J2K changes semantics when `if` does not have a body
- [`KT-18512`](https://youtrack.jetbrains.com/issue/KT-18512) J2K Incorrect null parameter conversion

### Tools. JPS

- [`KT-14848`](https://youtrack.jetbrains.com/issue/KT-14848) JPS: invalid compiler argument causes exception (see also EA-92062)
- [`KT-16057`](https://youtrack.jetbrains.com/issue/KT-16057) Provide better error message when the same compiler argument is set twice

### Tools. Maven

- [`KT-18224`](https://youtrack.jetbrains.com/issue/KT-18224) Maven compilation with JDK 9 fails with InaccessibleObjectException

### Tools. REPL

- [`KT-5620`](https://youtrack.jetbrains.com/issue/KT-5620) REPL: Support destructuring declarations
- [`KT-12564`](https://youtrack.jetbrains.com/issue/KT-12564) Kotlin REPL Doesn't Perform Many Checks
- [`KT-15172`](https://youtrack.jetbrains.com/issue/KT-15172) REPL: function declarations that contain empty lines throw error
- [`KT-18181`](https://youtrack.jetbrains.com/issue/KT-18181) REPL: support non-headless execution for Swing code
- [`KT-18349`](https://youtrack.jetbrains.com/issue/KT-18349) REPL: do not show warnings when there are errors

## Previous releases

This release also includes the fixes and improvements from the previous
[`1.1.3`](https://github.com/JetBrains/kotlin/blob/1.1.3/ChangeLog.md) release.