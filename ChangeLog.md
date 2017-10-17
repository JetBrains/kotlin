# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.2-Beta2

### Multiplatform projects

#### New Features

- [`KT-20398`](https://youtrack.jetbrains.com/issue/KT-20398) Do not highlight declarations with not implemented implementations with red during typing
- [`KT-19937`](https://youtrack.jetbrains.com/issue/KT-19937) Support "implement header class" quickfix for nested classes
- [`KT-20616`](https://youtrack.jetbrains.com/issue/KT-20616) Compiler options for `KotlinCompileCommon` task

#### Fixes

- [`KT-15522`](https://youtrack.jetbrains.com/issue/KT-15522) Treat header classes without explicit constructors as not having constructors at all
- [`KT-16099`](https://youtrack.jetbrains.com/issue/KT-16099) Do not require obvious override of super-interface methods in non-abstract header class
- [`KT-19170`](https://youtrack.jetbrains.com/issue/KT-19170) Forbid private expected declarations
- [`KT-20431`](https://youtrack.jetbrains.com/issue/KT-20431) Prohibit inheritance by delegation in 'expect' classes
- [`KT-20540`](https://youtrack.jetbrains.com/issue/KT-20540) Report errors about incompatible constructors of actual class
- [`KT-20657`](https://youtrack.jetbrains.com/issue/KT-20657) Actual annotation with all parameters that have default values doesn't match expected annotation with no-arg constructor
- [`KT-20680`](https://youtrack.jetbrains.com/issue/KT-20680) No actual class member: inconsistent modality check
- [`KT-18756`](https://youtrack.jetbrains.com/issue/KT-18756) multiplatform project: compilation error on implementation of extension property in javascript client module
- [`KT-17374`](https://youtrack.jetbrains.com/issue/KT-17374) Too many "header declaration has no implementation" inspection in IDE in a multi-platform project
- [`KT-18455`](https://youtrack.jetbrains.com/issue/KT-18455) Multiplatform project: show gutter Navigate to implementation on header side of method in the header class
- [`KT-19222`](https://youtrack.jetbrains.com/issue/KT-19222) Useless tooltip on a gutter icon for header declaration
- [`KT-20043`](https://youtrack.jetbrains.com/issue/KT-20043) multiplatform: No H gutter if a class has nested/inner classes inherited from it
- [`KT-20164`](https://youtrack.jetbrains.com/issue/KT-20164) Header/impl navigation does not work when impl is a typealias
- [`KT-20254`](https://youtrack.jetbrains.com/issue/KT-20254) multiplatform: there is no link between header and impl classes, if implementation has a constructor when header doesn't
- [`KT-20309`](https://youtrack.jetbrains.com/issue/KT-20309) multiplatform: ClassCastException on mouse hovering on the H gutter of the actual secondary constructor
- [`KT-20638`](https://youtrack.jetbrains.com/issue/KT-20638) Context menu in common module: NSEE: "Collection contains no element matching the predicate." at KotlinRunConfigurationProducerKt.findJvmImplementationModule()
- [`KT-18919`](https://youtrack.jetbrains.com/issue/KT-18919) multiplatform project: header keyword is lost on converting to object
- [`KT-20008`](https://youtrack.jetbrains.com/issue/KT-20008) multiplatform: Create header class implementation should add impl keyword at secondary constructors
- [`KT-20044`](https://youtrack.jetbrains.com/issue/KT-20044) multiplatform: Create header class implementation should add impl constructor at primary constructor
- [`KT-20135`](https://youtrack.jetbrains.com/issue/KT-20135) "Create header class implementation" should open created class in editor
- [`KT-20163`](https://youtrack.jetbrains.com/issue/KT-20163) multiplatform: it should be possible to create an implementation for overloaded method if for one method implementation is present already
- [`KT-20243`](https://youtrack.jetbrains.com/issue/KT-20243) multiplatform: quick fix Create header interface implementation should add impl keyword at interface members
- [`KT-20325`](https://youtrack.jetbrains.com/issue/KT-20325) multiplatform: Quick fix Create actual ... should specify correct classifier name for object, enum class and annotation class
- [`KT-20618`](https://youtrack.jetbrains.com/issue/KT-20618) Rename “implement” to “expectedBy” in gradle module dependency
- [`KT-20634`](https://youtrack.jetbrains.com/issue/KT-20634) Platform project project ‘:js’ implements non-common project project ‘:shared’

### Compiler

#### New Features

- [`KT-16028`](https://youtrack.jetbrains.com/issue/KT-16028) Allow to have different bodies of inline functions inlined depending on apiVersion

#### Performance Improvements

- [`KT-20462`](https://youtrack.jetbrains.com/issue/KT-20462) Don't create an array copy for '*<array-constructor-fun>(...)'

#### Fixes

- [`KT-13644`](https://youtrack.jetbrains.com/issue/KT-13644) Information from explicit cast should be used for type inference
- [`KT-14697`](https://youtrack.jetbrains.com/issue/KT-14697) Use-site targeted annotation is not correctly loaded from class file
- [`KT-17981`](https://youtrack.jetbrains.com/issue/KT-17981) Type parameter for catch parameter possible when exception is nested in generic, but fails in runtime
- [`KT-19251`](https://youtrack.jetbrains.com/issue/KT-19251) Stack spilling in constructor arguments breaks Quasar
- [`KT-20387`](https://youtrack.jetbrains.com/issue/KT-20387) Wrong argument generated for accessor call of a protected generic 'operator fun get/set' from base class with primitive type as type parameter
- [`KT-20491`](https://youtrack.jetbrains.com/issue/KT-20491) Incorrect synthetic accessor generated for a generic base class function specialized with primitive type
- [`KT-20651`](https://youtrack.jetbrains.com/issue/KT-20651) "Don't know how to generate outer expression" for enum-values with non-trivial self-closures
- [`KT-20752`](https://youtrack.jetbrains.com/issue/KT-20752) Do not register new kinds of smart casts for unstable values

### IDE

#### New Features

- [`KT-19146`](https://youtrack.jetbrains.com/issue/KT-19146) Parameter hints could be shown for annotation

#### Fixes

- [`KT-16926`](https://youtrack.jetbrains.com/issue/KT-16926) 'implement' dependency is not transitive when importing gradle project to IDEA
- [`KT-17369`](https://youtrack.jetbrains.com/issue/KT-17369) Remove allImplementingModules from module descriptor
- [`KT-19207`](https://youtrack.jetbrains.com/issue/KT-19207) "Configure Kotlin in project" should add "requires kotlin.stdlib" to module-info for Java 9 modules
- [`KT-19213`](https://youtrack.jetbrains.com/issue/KT-19213) Formatter/Code Style: space between type parameters and `where` is not inserted
- [`KT-19216`](https://youtrack.jetbrains.com/issue/KT-19216) Parameter name hints should not be shown for functional type invocation
- [`KT-20448`](https://youtrack.jetbrains.com/issue/KT-20448) Exception in UAST during reference search in J2K
- [`KT-20543`](https://youtrack.jetbrains.com/issue/KT-20543) java.lang.ClassCastException on usage of array literals in Spring annotation
- [`KT-20709`](https://youtrack.jetbrains.com/issue/KT-20709) Loop in parent structure when converting a LITERAL_STRING_TEMPLATE_ENTRY

### IDE. Completion

- [`KT-17165`](https://youtrack.jetbrains.com/issue/KT-17165) Support array literals in annotations in completion

### IDE. Debugger

- [`KT-18775`](https://youtrack.jetbrains.com/issue/KT-18775) Evaluate expression doesn't allow access to properties of private nested objects, including companion

### IDE. Inspections and Intentions

#### New Features

- [`KT-20108`](https://youtrack.jetbrains.com/issue/KT-20108) Support "add requires directive to module-info.java" quick fix on usages of non-required modules in Kotlin sources
- [`KT-20410`](https://youtrack.jetbrains.com/issue/KT-20410) Add inspection for listOf().filterNotNull() to replace it with listOfNotNull()

#### Fixes

- [`KT-16636`](https://youtrack.jetbrains.com/issue/KT-16636) Remove parentheses after deleting the last unused constructor parameter
- [`KT-18549`](https://youtrack.jetbrains.com/issue/KT-18549) "Add type" quick fix adds non-primitive Array type for annotation parameters
- [`KT-18631`](https://youtrack.jetbrains.com/issue/KT-18631) Inspection to convert emptyArray() to empty literal does not work
- [`KT-18773`](https://youtrack.jetbrains.com/issue/KT-18773) Disable "Replace camel-case name with spaces" intention for JS and common projects
- [`KT-20183`](https://youtrack.jetbrains.com/issue/KT-20183) AE “Classifier descriptor of a type should be of type ClassDescriptor” on adding element to generic collection in function
- [`KT-20315`](https://youtrack.jetbrains.com/issue/KT-20315) "call chain on collection type may be simplified" generates code that does not compile

### JavaScript

#### Fixes

- [`KT-8285`](https://youtrack.jetbrains.com/issue/KT-8285) JS: don't generate tmp when only need one component
- [`KT-8374`](https://youtrack.jetbrains.com/issue/KT-8374) JS: some Double values converts to Int differently on JS and JVM
- [`KT-14549`](https://youtrack.jetbrains.com/issue/KT-14549) JS: Non-local returns from secondary constructors don't work
- [`KT-15294`](https://youtrack.jetbrains.com/issue/KT-15294) JS: parse error in `js()` function
- [`KT-17629`](https://youtrack.jetbrains.com/issue/KT-17629) JS: Equals function (==) returns true for all primitive numeric types
- [`KT-17760`](https://youtrack.jetbrains.com/issue/KT-17760) JS: Nothing::class throws error
- [`KT-17933`](https://youtrack.jetbrains.com/issue/KT-17933) JS: toString, hashCode method and simplename property of KClass return senseless results for some classes
- [`KT-18010`](https://youtrack.jetbrains.com/issue/KT-18010) JS: JsName annotation in interfaces can cause runtime exception
- [`KT-18063`](https://youtrack.jetbrains.com/issue/KT-18063) Inlining does not work properly in JS for suspend functions from another module
- [`KT-18548`](https://youtrack.jetbrains.com/issue/KT-18548) JS: wrong string interpolation with generic or Any parameters
- [`KT-19772`](https://youtrack.jetbrains.com/issue/KT-19772) JS: wrong boxing behavior for open val and final fun inside open class
- [`KT-19794`](https://youtrack.jetbrains.com/issue/KT-19794) runtime crash with empty object (Javascript)
- [`KT-19818`](https://youtrack.jetbrains.com/issue/KT-19818) JS: generate paths relative to .map file by default (unless "-source-map-prefix" is used)
- [`KT-19906`](https://youtrack.jetbrains.com/issue/KT-19906) JS: rename compiler option "-source-map-source-roots" to avoid misleading since sourcemaps have field called "sourceRoot"
- [`KT-20287`](https://youtrack.jetbrains.com/issue/KT-20287) Functions don't actually return Unit in Kotlin-JS -> unexpected null problems vs JDK version
- [`KT-20451`](https://youtrack.jetbrains.com/issue/KT-20451) KotlinJs - interface function with default parameter, overridden by implementor, can't be found at runtime
- [`KT-20527`](https://youtrack.jetbrains.com/issue/KT-20527) JS: use prototype chain to check that object implements kotlin interface
- [`KT-20650`](https://youtrack.jetbrains.com/issue/KT-20650) JS: compiler crashes in Java 9 with NoClassDefFoundError
- [`KT-20653`](https://youtrack.jetbrains.com/issue/KT-20653) JS: compiler crashes in Java 9 with TranslationRuntimeException

### Language design

- [`KT-20171`](https://youtrack.jetbrains.com/issue/KT-20171) Deprecate assigning single elements to varargs in named form

### Libraries

- [`KT-19696`](https://youtrack.jetbrains.com/issue/KT-19696) Provide a way to write multiplatform tests
- [`KT-18961`](https://youtrack.jetbrains.com/issue/KT-18961) Closeable.use should call addSuppressed
- [`KT-19494`](https://youtrack.jetbrains.com/issue/KT-19494) Add @Ignore annotation to kotlin.test
- [`KT-20585`](https://youtrack.jetbrains.com/issue/KT-20585) Add marker intrinsics to kotlin-stdlib to be available for cases like apiVersionIsAtLeast

### Tools. Gradle

- [`KT-20553`](https://youtrack.jetbrains.com/issue/KT-20553) Rename warningsAsErrors compiler option in gradle
- [`KT-20217`](https://youtrack.jetbrains.com/issue/KT-20217) gradle + kotlin projects: don't create java source roots

### Tools. Incremental Compile

- [`KT-20654`](https://youtrack.jetbrains.com/issue/KT-20654) AndroidStudio: NSME “PsiJavaModule.getName()Ljava/lang/String” on calling simple Kotlin functions like println(), listOf()

### Binary Metadata

- [`KT-20547`](https://youtrack.jetbrains.com/issue/KT-20547) Write pre-release flag into class files if language version > LATEST_STABLE

## Previous releases

This release also includes the fixes and improvements from the previous
[`1.2-Beta`](https://github.com/JetBrains/kotlin/blob/1.2-Beta/ChangeLog.md) release.