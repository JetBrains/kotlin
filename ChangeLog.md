# CHANGELOG

## 1.3.70

### Compiler

- [`KT-31242`](https://youtrack.jetbrains.com/issue/KT-31242) "Can't find enclosing method" ProGuard compilation exception with inline and crossinline
- [`KT-31923`](https://youtrack.jetbrains.com/issue/KT-31923) Outer finally block inserted before return instruction is not excluded from catch interval of inner try (without finally) block
- [`KT-32435`](https://youtrack.jetbrains.com/issue/KT-32435) New inference preserves platform types while old inference can substitute them with the nullable result type
- [`KT-34060`](https://youtrack.jetbrains.com/issue/KT-34060) UNUSED_PARAMETER is not reported on unused parameters of non-operator getValue/setValue/prodiveDelegate functions
- [`KT-34395`](https://youtrack.jetbrains.com/issue/KT-34395) KtWhenConditionInRange.isNegated() doesn't work
- [`KT-34648`](https://youtrack.jetbrains.com/issue/KT-34648) Support custom messages for @RequiresOptIn-marked annotations
- [`KT-34888`](https://youtrack.jetbrains.com/issue/KT-34888) Kotlin REPL ignores compilation errors in class declaration
- [`KT-35035`](https://youtrack.jetbrains.com/issue/KT-35035) Incorrect state-machine generated for suspend lambda inside inline lambda
- [`KT-35262`](https://youtrack.jetbrains.com/issue/KT-35262) Suspend function with Unit return type returns non-unit value if it is derived from function with non-unit return type
- [`KT-35843`](https://youtrack.jetbrains.com/issue/KT-35843) Emit type annotations in JVM bytecode with target 1.8+ on basic constructions

### IDE

- [`KT-24399`](https://youtrack.jetbrains.com/issue/KT-24399) No scrollbar in Kotlin compiler settings
- [`KT-33939`](https://youtrack.jetbrains.com/issue/KT-33939) Copy action leads to freezes
- [`KT-35673`](https://youtrack.jetbrains.com/issue/KT-35673) ClassCastException on destructuring declaration with annotation
- [`KT-36008`](https://youtrack.jetbrains.com/issue/KT-36008) IDEA 2020.1: Fix NSME "com.intellij.openapi.progress.util.ProgressIndicatorUtils.awaitWithCheckCanceled(Future)"

### IDE. Completion

- [`KT-23026`](https://youtrack.jetbrains.com/issue/KT-23026) Code completion: Incorrect `const` in class declaration line
- [`KT-23834`](https://youtrack.jetbrains.com/issue/KT-23834) Code completion and auto import do not suggest an extension that differs from member only in type parameters
- [`KT-29840`](https://youtrack.jetbrains.com/issue/KT-29840) 'const' is suggested inside the class body, despite it's illegal

### IDE. Gradle

- [`KT-35442`](https://youtrack.jetbrains.com/issue/KT-35442) KotlinMPPGradleModelBuilder shows warnings on import because it can't find  a not existing directory

### IDE. Gradle. Script

- [`KT-34795`](https://youtrack.jetbrains.com/issue/KT-34795) Gradle Kotlin DSL new project template: don't use `setUrl` syntax in `settings.gradle.kts` `pluginManagement` block
- [`KT-35268`](https://youtrack.jetbrains.com/issue/KT-35268) .gradle.kts: don't load script's configurations that are not connected to any Gradle project
- [`KT-35563`](https://youtrack.jetbrains.com/issue/KT-35563) Track script modifications between IDE restarts

### IDE. Inspections and Intentions

- [`KT-35242`](https://youtrack.jetbrains.com/issue/KT-35242) Text-range based inspection range shifts wrongly due to incremental analysis of whitespace and comments
- [`KT-35837`](https://youtrack.jetbrains.com/issue/KT-35837) Editing Introduce import alias does not affect KDoc
- [`KT-36018`](https://youtrack.jetbrains.com/issue/KT-36018) 'Missing visibility' and 'missing explicit return type' compiler and IDE diagnostics for explicit API mode
- [`KT-36020`](https://youtrack.jetbrains.com/issue/KT-36020) Intention 'Add public modifier' is not available for highlighted declaration in explicit API mode
- [`KT-36021`](https://youtrack.jetbrains.com/issue/KT-36021) KDoc shouldn't be highlighted on 'visibility must be specified' warning in explicit API mode

### IDE. Navigation

- [`KT-35310`](https://youtrack.jetbrains.com/issue/KT-35310) PIEAE: "During querying provider Icon preview" at ClsJavaCodeReferenceElementImpl.multiResolve() on navigation to Kotlin declaration

### IDE. Run Configurations

- [`KT-34503`](https://youtrack.jetbrains.com/issue/KT-34503) "Nothing here" is shown as a drop-down list for "Run test" gutter icon for a multiplatform test with expect/actual parts in platform-agnostic code
- [`KT-35480`](https://youtrack.jetbrains.com/issue/KT-35480) "Nothing here" is shown as a drop-down list for "Run test" gutter icon for a multiplatform test with object in JS and Native code

### IDE. Script

- [`KT-35886`](https://youtrack.jetbrains.com/issue/KT-35886) UI Freeze: ScriptClassRootsCache.hasNotCachedRoots 25 seconds

### IDE. Wizards

#### New Features

- [`KT-36043`](https://youtrack.jetbrains.com/issue/KT-36043) Gradle, JS: Add continuous-mode run configuration in New Project Wizard templates

#### New Project Wizard

- [`KT-35584`](https://youtrack.jetbrains.com/issue/KT-35584) Module names restrictions are too strong with no reason
- [`KT-35690`](https://youtrack.jetbrains.com/issue/KT-35690) Artifact and group fields are mixed up
- [`KT-35694`](https://youtrack.jetbrains.com/issue/KT-35694) `settings.gradle.kts` are created even for Groovy DSL
- [`KT-35695`](https://youtrack.jetbrains.com/issue/KT-35695) `kotlin ()` call used for dependencies in non-MPP Groovy-DSL JVM project
- [`KT-35710`](https://youtrack.jetbrains.com/issue/KT-35710) Non-Java source/resource roots are created for Kotlin/JVM JPS
- [`KT-35712`](https://youtrack.jetbrains.com/issue/KT-35712) Source root templates: switching focus from root reverts custom settings to default
- [`KT-35713`](https://youtrack.jetbrains.com/issue/KT-35713) Custom settings for project name, artifact and group ID are reverted to default on Previous/Next
- [`KT-35711`](https://youtrack.jetbrains.com/issue/KT-35711) Maven: "Kotlin Test framework" template adds wrong dependency
- [`KT-35715`](https://youtrack.jetbrains.com/issue/KT-35715) Maven: custom repository required for template (ktor) is not added to pom.xml
- [`KT-35718`](https://youtrack.jetbrains.com/issue/KT-35718) Gradle: ktor: not existing repository is added
- [`KT-35719`](https://youtrack.jetbrains.com/issue/KT-35719) Multiplatform library: entryPoint specifies not existing class name
- [`KT-35720`](https://youtrack.jetbrains.com/issue/KT-35720) Multiplatform library: Groovy DSL: improve the script for nativeTarget calculation

### Libraries

- [`KT-15363`](https://youtrack.jetbrains.com/issue/KT-15363) Builder functions for basic containers
- [`KT-21327`](https://youtrack.jetbrains.com/issue/KT-21327) Add Deque & ArrayDeque to Kotlin standard library
- [`KT-33141`](https://youtrack.jetbrains.com/issue/KT-33141) UnderMigration annotation is defined in Kotlin, but supposed to be used from Java
- [`KT-35347`](https://youtrack.jetbrains.com/issue/KT-35347) Create method Collection.randomOrNull()
- [`KT-36118`](https://youtrack.jetbrains.com/issue/KT-36118) Provide API for subtyping relationship between CoroutineContextKey and elements associated with this key

### Tools. Gradle

- [`KT-25206`](https://youtrack.jetbrains.com/issue/KT-25206) Delegate build/run to gradle results regularly in cannot delete proto.tab.value.s

### Tools. Gradle. JS

- [`KT-31894`](https://youtrack.jetbrains.com/issue/KT-31894) `browserRun` makes the build fail if no Kotlin sources are present
- [`KT-35599`](https://youtrack.jetbrains.com/issue/KT-35599) Actualize Node and Yarn versions in 1.3.70

### Tools. Gradle. Multiplatform

- [`KT-31570`](https://youtrack.jetbrains.com/issue/KT-31570) Deprecate Kotlin 1.2.x MPP Gradle plugins

### Tools. Gradle. Native

- [`KT-29395`](https://youtrack.jetbrains.com/issue/KT-29395) Allow setting custom destination directory for Kotlin/Native binaries
- [`KT-31542`](https://youtrack.jetbrains.com/issue/KT-31542) Allow changing the name of a framework created by CocoaPods Gradle plugin
- [`KT-32750`](https://youtrack.jetbrains.com/issue/KT-32750) Support subspecs in CocoaPods plugin
- [`KT-35352`](https://youtrack.jetbrains.com/issue/KT-35352) Support exporting K/N dependencies to shared and static libraries
- [`KT-35934`](https://youtrack.jetbrains.com/issue/KT-35934) Spaces are not escaped in K/N compiler parameters
- [`KT-35958`](https://youtrack.jetbrains.com/issue/KT-35958) Compiling test sources with no sources in main roots halts the Gradle daemon

### Tools. J2K

- [`KT-18001`](https://youtrack.jetbrains.com/issue/KT-18001) Multi-line comments parsed inside Kdoc comments
- [`KT-33637`](https://youtrack.jetbrains.com/issue/KT-33637) Property with getter is converted into incompailable code if backing field was not generated
- [`KT-35081`](https://youtrack.jetbrains.com/issue/KT-35081) Invalid code with block comment (Javadoc)
- [`KT-35395`](https://youtrack.jetbrains.com/issue/KT-35395) UninitializedPropertyAccessException through `org.jetbrains.kotlin.nj2k.conversions.ImplicitCastsConversion` when anonymous inner class passes itself as argument to outer method
- [`KT-35431`](https://youtrack.jetbrains.com/issue/KT-35431) "Invalid PSI class com.intellij.psi.PsiLambdaParameterType" with lambda argument in erroneous code
- [`KT-35476`](https://youtrack.jetbrains.com/issue/KT-35476) Expression with compound assignment logical operator is changing operator precedence without parentheses
- [`KT-35478`](https://youtrack.jetbrains.com/issue/KT-35478) Single line comment before constructor results in wrong code
- [`KT-35739`](https://youtrack.jetbrains.com/issue/KT-35739) Line break is not inserted for private property getter
- [`KT-35831`](https://youtrack.jetbrains.com/issue/KT-35831) Error on inserting plain text with \r char

### Tools. Scripts

- [`KT-34716`](https://youtrack.jetbrains.com/issue/KT-34716) Implement default cache in main-kts

### Tools. kapt

- [`KT-34569`](https://youtrack.jetbrains.com/issue/KT-34569) Kapt doesn't handle methods with both the @Override annotation and `override` keyword
- [`KT-35181`](https://youtrack.jetbrains.com/issue/KT-35181) Make Kapt Gradle tasks compatible with instant execution