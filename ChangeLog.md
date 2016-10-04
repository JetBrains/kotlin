# CHANGELOG

<!-- Find: ([^\`/\[])(KT-\d+) -->
<!-- Replace: $1[`$2`](https://youtrack.jetbrains.com/issue/$2) -->

## 1.1-M02 (EAP-2)

### Language features

+ **Destructuring for lambdas** ([proposal](https://github.com/Kotlin/KEEP/issues/32))

    Current limitations:

    - Nested destructuring is not supported
    - Destructuring in named functions/constructors is not supported
        
### Compiler

#### Smart cast enhancements
- [`KT-2127`](https://youtrack.jetbrains.com/issue/KT-2127) Smart cast receiver to not null after a not null safe call
- [`KT-6840`](https://youtrack.jetbrains.com/issue/KT-6840) Make data flow information the same for assigned and assignee
- [`KT-13426`](https://youtrack.jetbrains.com/issue/KT-13426) Fix exception when smartcast on both dispatch & extension receiver

#### Bound references related issues
- [`KT-12995`](https://youtrack.jetbrains.com/issue/KT-12995) Do not skip generation of the left-hand side for intrinsic bound references and class literals
- [`KT-13075`](https://youtrack.jetbrains.com/issue/KT-13075) Fix codegen for bound class reference
- [`KT-13110`](https://youtrack.jetbrains.com/issue/KT-13110) Fix type mismatch error on class literal with integer receiver expression
- [`KT-13172`](https://youtrack.jetbrains.com/issue/KT-13172) Report error on "this::class" in super constructor call
- [`KT-13271`](https://youtrack.jetbrains.com/issue/KT-13271) Fix incorrect unsupported error on synthetic extension call on LHS of ::
- [`KT-13367`](https://youtrack.jetbrains.com/issue/KT-13367) Inline bound callable reference if it's used only as a lambda

#### Coroutines related issues
- [`KT-13156`](https://youtrack.jetbrains.com/issue/KT-13156) Do not execute last Unit-typed coroutine statement twice
- [`KT-13246`](https://youtrack.jetbrains.com/issue/KT-13246) Fix VerifyError with coroutines on Dalvik
- [`KT-13289`](https://youtrack.jetbrains.com/issue/KT-13289) Fix VerifyError with coroutines: Bad type on operand stack
- [`KT-13409`](https://youtrack.jetbrains.com/issue/KT-13409) Fix generic variable spilling with coroutines
- [`KT-13531`](https://youtrack.jetbrains.com/issue/KT-13531) Fix ClassCastException when coercion to Unit interacts with generic await() and coroutines
- Prohibit `Continuation<*>` as a last parameter of suspend functions

#### Typealises related issues
- [`KT-13200`](https://youtrack.jetbrains.com/issue/KT-13200) Fix incorrect number of required type arguments reported on typealias
- [`KT-13181`](https://youtrack.jetbrains.com/issue/KT-13181) Fix unresolved reference for a type alias from a different module
- [`KT-13161`](https://youtrack.jetbrains.com/issue/KT-13161) Support java static methods calls with typealiases
- [`KT-13835`](https://youtrack.jetbrains.com/issue/KT-13835) Do not lose nullability information  while expanding type alias in projection position
- [`KT-13422`](https://youtrack.jetbrains.com/issue/KT-13422) Prohibit usage of type alias to exception class as an object in 'throw' expression 
- [`KT-13735`](https://youtrack.jetbrains.com/issue/KT-13735) Fix NoSuchMethodError for generic typealias access
- [`KT-13513`](https://youtrack.jetbrains.com/issue/KT-13513) Support SAM constructors for aliased java functional types
- [`KT-13822`](https://youtrack.jetbrains.com/issue/KT-13822) Fix exception for start-projection of a type alias

#### JDK dependent built-in classes related issues
- [`KT-13209`](https://youtrack.jetbrains.com/issue/KT-13209) Change first parameter's type of Map.getOrDefault to K instead of Any
- [`KT-13069`](https://youtrack.jetbrains.com/issue/KT-13069) Do not emit invalid DefaultImpls delegation when interface extends MutableMap with JDK8

#### `data` classes and inheritance
- [`KT-11306`](https://youtrack.jetbrains.com/issue/KT-11306) Allow data classes to implement equals/hashCode/toString from base classes

#### Various JVM code generation issues
- [`KT-13182`](https://youtrack.jetbrains.com/issue/KT-13182) Fix compiler internal error at inline
- [`KT-13757`](https://youtrack.jetbrains.com/issue/KT-13757) Prohibit referencing nested classes by name with $

### JS

#### Bugfixes
- [`KT-13544`](https://youtrack.jetbrains.com/issue/KT-13544) Support typealiases in JS
- [`KT-13836`](https://youtrack.jetbrains.com/issue/KT-13836) Calling secondary constructor via type alias generates incorrect call in JS

#### Library updates
- [`KT-12386`](https://youtrack.jetbrains.com/issue/KT-12386) Rewrite JS collections Kotlin
- [`KT-7473`](https://youtrack.jetbrains.com/issue/KT-7473) Make AbstractCollection.equals check object type
- [`KT-7809`](https://youtrack.jetbrains.com/issue/KT-7809) Make Collection implementations conform to their declared interfaces
- [`KT-9108`](https://youtrack.jetbrains.com/issue/KT-9108) Add toHashMap extension for Maps
- [`KT-13429`](https://youtrack.jetbrains.com/issue/KT-13429) Make 'remove' on fresh iterator throw exception  instead of removing last element
- [`KT-13459`](https://youtrack.jetbrains.com/issue/KT-13459) Make JS implementation of ArrayList::add(ind, element) check range
- [`KT-8724`](https://youtrack.jetbrains.com/issue/KT-8724) Fix MutableIterator.remove() for HashMap
- [`KT-10786`](https://youtrack.jetbrains.com/issue/KT-10786) Make Map.keys return snapshot instead of view

### Standard Library

#### Enhancements
- [`KT-12762`](https://youtrack.jetbrains.com/issue/KT-12762) Make `kotlin.ranges.until` return an empty range for "illegal" 'to' parameter
- [`KT-12894`](https://youtrack.jetbrains.com/issue/KT-12894) Allow nullable receiver for `use` extension

### Reflection

#### New features
- [`KT-8998`](https://youtrack.jetbrains.com/issue/KT-8998) Introduce comprehensive API to work with KType instances 
- [`KT-10447`](https://youtrack.jetbrains.com/issue/KT-10447) Provide a way to check if a KClass is a data class
- [`KT-11284`](https://youtrack.jetbrains.com/issue/KT-11284) Add KClass<T>.cast extension
- [`KT-13106`](https://youtrack.jetbrains.com/issue/KT-13106) Support annotation constructors in reflection

#### Optimizations
- [`KT-10651`](https://youtrack.jetbrains.com/issue/KT-10651) Optimize KClass.simpleName

### IDE

###### New features
- [`KT-12903`](https://youtrack.jetbrains.com/issue/KT-12903) Implement "Inline type alias" refactoring
- [`KT-12902`](https://youtrack.jetbrains.com/issue/KT-12902) Implement "Introduce type alias" refactoring
- [`KT-12904`](https://youtrack.jetbrains.com/issue/KT-12904) Implement "Create type alias from usage" quick fix

###### Issues fixed
- [`KT-13004`](https://youtrack.jetbrains.com/issue/KT-13004) Support bound method references in completion
- [`KT-13242`](https://youtrack.jetbrains.com/issue/KT-13242) Suggest 'typealias' keyword in completion
- [`KT-13244`](https://youtrack.jetbrains.com/issue/KT-13244) Override/Implement Members: Do not expand type aliases in the generated members
- [`KT-13611`](https://youtrack.jetbrains.com/issue/KT-13611) Go to Class: Fix presentation of type aliases
- [`KT-13759`](https://youtrack.jetbrains.com/issue/KT-13759) Rename: Process object-wrapping alias references
- [`KT-13955`](https://youtrack.jetbrains.com/issue/KT-13955) Find Usages: Add special type for usages inside of type aliases
- [`KT-13479`](https://youtrack.jetbrains.com/issue/KT-13479) Support navigation to type aliases from binaries

## 1.1-M01 (EAP-1)

### Language features

+ **Coroutines (async/await, generators)** ([proposal](https://github.com/Kotlin/kotlin-coroutines))

    Current limitations:

    - for some cases type inference is not supported yet
    - limited IDE support
    - allowed only one `handleResult` function: [design](https://github.com/Kotlin/kotlin-coroutines/blob/master/kotlin-coroutines-informal.md#result-handlers)
    - handling `finally` blocks is not supported: [issue](https://github.com/Kotlin/kotlin-coroutines/issues/1)

+ **Bound callable references** ([proposal](https://github.com/Kotlin/KEEP/issues/5))

+ **Type aliases** ([proposal](https://github.com/Kotlin/KEEP/issues/4))

    Current limitations:
    - type alias constructors for inner classes are not supported yet
    - annotations on type alias are not supported yet
    - limited IDE support

+ **Local delegated properties** ([proposal](https://github.com/Kotlin/KEEP/issues/25))

+ **JDK dependent built-in classes** ([proposal](https://github.com/Kotlin/KEEP/issues/30))

+ **Sealed class inheritors in the same file** ([proposal](https://github.com/Kotlin/KEEP/issues/29))
+ **Allow base classes for data classes** ([proposal](https://github.com/Kotlin/KEEP/issues/31))


### Scripting

- Implement support for [Script Definition Template](https://github.com/Kotlin/KEEP/blob/da9f3ec5f78429e7560bfc284cb7f52e02282b1f/proposals/script-definition-template.md)
and related functionality, except the following parts:
  - automatic script templates discovery is not implemented
  - `@file:ScriptTemplate` annotation is not supported
  - the parameters `javaHome` and `scripts` from `KotlinScriptExternalDependencies` are not used yet
- Implement support for custom template-based scripts in IDEA: resolving, completion and navigation to symbols from script classpath and sources
- Implement GradleScriptTemplatesProvider extension that supplies a script template if gradle with
[kotlin script support](https://github.com/gradle/gradle-script-kotlin) is used in the project


### Compiler

###### Issues fixed
- [`KT-4779`](https://youtrack.jetbrains.com/issue/KT-4779) Generate default methods for implementations in interfaces
- [`KT-11780`](https://youtrack.jetbrains.com/issue/KT-11780) Fixed incorrect "No cast needed" warning
- [`KT-12156`](https://youtrack.jetbrains.com/issue/KT-12156) Fixed incorrect error on `inline` modifier inside final class
- [`KT-12358`](https://youtrack.jetbrains.com/issue/KT-12358) Report missing error "Abstract member not implemented" when a fake method of 'Any' is inherited from an interface
- [`KT-6206`](https://youtrack.jetbrains.com/issue/KT-6206) Generate equals/hashCode/toString in data class always unless it'll cause a JVM signature clash error
- [`KT-8990`](https://youtrack.jetbrains.com/issue/KT-8990) Fixed incorrect error "virtual member hidden" for a private method of an inner class
- [`KT-12429`](https://youtrack.jetbrains.com/issue/KT-12429) Fixed visibility checks for annotation usage on top-level declarations
- [`KT-5068`](https://youtrack.jetbrains.com/issue/KT-5068) Introduced a special diagnostic message for "type mismatch" errors such as `fun f(): Int = { 1 }`.

### Standard Library

- [`KT-8254`](https://youtrack.jetbrains.com/issue/KT-8254) Provide standard library supplement artifacts for using with JDK 7 and 8.
These artifacts include extensions for the types available in the latter JDKs, such as `AutoCloseable.use` ([`KT-5899`](https://youtrack.jetbrains.com/issue/KT-5899)) or `Stream.toList`.
- [`KT-12753`](https://youtrack.jetbrains.com/issue/KT-12753) Provide an access to named group matches of `Regex` match result (for JDK 8 only).
- Add `assertFails` overload with message to kotlin-test.


### IDE

###### New features

+ [`KT-12019`](https://youtrack.jetbrains.com/issue/KT-12019) Introduce "redundant `if`" inspection

###### Issues fixed

+ [`KT-12389`](https://youtrack.jetbrains.com/issue/KT-12389) Do not exit from REPL when toString() of user class throws an exception
+ [`KT-12129`](https://youtrack.jetbrains.com/issue/KT-12129) Fixed link on api reference page in KDoc

## 1.0.5

### IDE

- [`KT-9125`](https://youtrack.jetbrains.com/issue/KT-9125) Support Type Hierarchy on references inside of super type call entries
- [`KT-13542`](https://youtrack.jetbrains.com/issue/KT-13542) Rename: Do not search parameter text occurrences outside of its containing declaration
- [`KT-8672`](https://youtrack.jetbrains.com/issue/KT-8672) Rename: Optimize search of parameter references in calls with named arguments
- [`KT-9285`](https://youtrack.jetbrains.com/issue/KT-9285) Rename: Optimize search of private class members
- [`KT-13589`](https://youtrack.jetbrains.com/issue/KT-13589) Use TODO() consistently in implementation stubs
- [`KT-13630`](https://youtrack.jetbrains.com/issue/KT-13630) Do not show Change Signature dialog when applying "Remove parameter" quick-fix
- Re-highlight only single function after local modifications
- [`KT-13474`](https://youtrack.jetbrains.com/issue/KT-13474) Fix performance of typing super call lambda
- Show "Variables and values captured in a closure" highlighting only for usages
- [`KT-13838`](https://youtrack.jetbrains.com/issue/KT-13838) Add file name to the presentation of private top-level declaration (Go to symbol, etc.)
- [`KT-14096`](https://youtrack.jetbrains.com/issue/KT-14096) Rename: When renaming Kotlin file outside of source root do not rename its namesake in a source root
- [`KT-13928`](https://youtrack.jetbrains.com/issue/KT-13928) Move Inner Class to Upper Level: Fix replacement of outer class instances used in inner class constructor calls
- [`KT-12556`](https://youtrack.jetbrains.com/issue/KT-12556) Allow using whitespaces and other symbols in "Generate -> Test Function" dialog
- [`KT-14122`](https://youtrack.jetbrains.com/issue/KT-14122) Generate 'toString()': Permit for data classes
- [`KT-12398`](https://youtrack.jetbrains.com/issue/KT-12398) Call Hierarchy: Show Kotlin usages of Java methods

#### Intention actions, inspections and quickfixes

- [`KT-9490`](https://youtrack.jetbrains.com/issue/KT-9490) Convert receiver to parameter: use template instead of the dialog
- [`KT-11483`](https://youtrack.jetbrains.com/issue/KT-11483) Move to Companion: Do not use qualified names as labels
- [`KT-13874`](https://youtrack.jetbrains.com/issue/KT-13874) Move to Companion: Fix AssertionError on running refactoring from Conflicts View
- [`KT-13883`](https://youtrack.jetbrains.com/issue/KT-13883) Move to Companion Object: Fix exception when applied to class
- [`KT-13876`](https://youtrack.jetbrains.com/issue/KT-13876) Move to Companion Object: Forbid for functions/properties referencing type parameters of the containing class
- [`KT-13877`](https://youtrack.jetbrains.com/issue/KT-13877) Move to Companion Object: Warn if companion object already contains function with the same signature
- [`KT-13933`](https://youtrack.jetbrains.com/issue/KT-13933) Convert Parameter to Receiver: Do not qualify companion members with labeled 'this'
- [`KT-13942`](https://youtrack.jetbrains.com/issue/KT-13942) Redundant 'toString()' in String Template: Disable for qualified expressions with 'super' receiver
- [`KT-13878`](https://youtrack.jetbrains.com/issue/KT-13878) Remove Redundant Receiver Parameter: Fix exception receiver removal
- [`KT-14143`](https://youtrack.jetbrains.com/issue/KT-14143) Create from Usages: Do not suggest on type-mismatched expressions which are not call arguments

##### New features

- [`KT-11525`](https://youtrack.jetbrains.com/issue/KT-11525) Implement "Create type parameter" quickfix
- [`KT-9931`](https://youtrack.jetbrains.com/issue/KT-9931) Implement "Remove unused assignment" quickfix

#### Refactorings

- [`KT-13535`](https://youtrack.jetbrains.com/issue/KT-13535) Pull Up: Remove visibility modifiers on adding 'override'
- [`KT-13216`](https://youtrack.jetbrains.com/issue/KT-13216) Move: Report separate conflicts for each property accessor
- [`KT-13216`](https://youtrack.jetbrains.com/issue/KT-13216) Move: Forbid moving of enum entries
- [`KT-13553`](https://youtrack.jetbrains.com/issue/KT-13553) Move: Do not show directory selection dialog if target directory is already specified by drag-and-drop
- [`KT-8867`](https://youtrack.jetbrains.com/issue/KT-8867) Rename: Rename all overridden members if user chooses to refactor base declaration(s)
- Pull Up: Drop 'override' modifier if moved member doesn't override anything
- [`KT-13660`](https://youtrack.jetbrains.com/issue/KT-13660) Move: Do not drop object receivers when calling variable of extension functional type
- [`KT-13903`](https://youtrack.jetbrains.com/issue/KT-13903) Move: Remove companion object which becomes empty after the move
- [`KT-13916`](https://youtrack.jetbrains.com/issue/KT-13916) Move: Report visibility conflicts in import directives
- [`KT-13906`](https://youtrack.jetbrains.com/issue/KT-13906) Move Nested Class to Upper Level: Do not show directory selection dialog twice
- [`KT-13901`](https://youtrack.jetbrains.com/issue/KT-13901) Move: Do not ignore target directory selected in the dialog (DnD mode)
- [`KT-13904`](https://youtrack.jetbrains.com/issue/KT-13904) Move Nested Class to Upper Level: Preserve state of "Search in comments"/"Search for text occurrences" checkboxes
- [`KT-13909`](https://youtrack.jetbrains.com/issue/KT-13909) Move Files/Directories: Fix behavior of "Open moved files in editor" checkbox
- [`KT-14004`](https://youtrack.jetbrains.com/issue/KT-14004) Introduce Variable: Fix exception on trying to extract variable of functional type
- [`KT-13726`](https://youtrack.jetbrains.com/issue/KT-13726) Move: Fix bogus conflicts due to references resolving to wrong library version
- [`KT-14114`](https://youtrack.jetbrains.com/issue/KT-14114) Move: Fix exception on moving Kotlin file without declarations
- [`KT-14157`](https://youtrack.jetbrains.com/issue/KT-14157) Rename: Rename do-while loop variables in the loop condition

##### New features

- [`KT-13155`](https://youtrack.jetbrains.com/issue/KT-13155) Implement "Introduce Type Parameter" refactoring
- [`KT-11017`](https://youtrack.jetbrains.com/issue/KT-11017) Implement "Extract Superclass" refactoring
- [`KT-11017`](https://youtrack.jetbrains.com/issue/KT-11017) Implement "Extract Interface" refactoring
Pull Up: Support properties declared in the primary constructor
Pull Up: Support members declared in the companion object of the original class
Pull Up: Show member dependencies in the refactoring dialog
- [`KT-9485`](https://youtrack.jetbrains.com/issue/KT-9485) Push Down: Support moving members from Java to Kotlin class
- [`KT-13963`](https://youtrack.jetbrains.com/issue/KT-13963) Rename: Implement popup chooser for overriding members

#### Android Lint

###### Issues fixed

- [`KT-12022`](https://youtrack.jetbrains.com/issue/KT-12022) Report lint warnings even when file contains errors

## 1.0.4

### Compiler

#### Analysis & diagnostics

- [`KT-10968`](https://youtrack.jetbrains.com/issue/KT-10968), [`KT-11075`](https://youtrack.jetbrains.com/issue/KT-11075), [`KT-12286`](https://youtrack.jetbrains.com/issue/KT-12286) Type inference of callable references
- [`KT-11892`](https://youtrack.jetbrains.com/issue/KT-11892) Report error on qualified super call to a supertype extended by a different supertype
- [`KT-12875`](https://youtrack.jetbrains.com/issue/KT-12875) Report error on incorrect call of member extension invoke
- [`KT-12847`](https://youtrack.jetbrains.com/issue/KT-12847) Report error on accessing protected property setter from super class' companion
- [`KT-12322`](https://youtrack.jetbrains.com/issue/KT-12322) Overload resolution ambiguity with constructor reference when class has a companion object
- [`KT-11440`](https://youtrack.jetbrains.com/issue/KT-11440) Overload resolution ambiguity on specialized Map.put implementation from Java
- [`KT-11389`](https://youtrack.jetbrains.com/issue/KT-11389) Runtime exception when calling Java primitive overloadings
- [`KT-8200`](https://youtrack.jetbrains.com/issue/KT-8200) Exception when using non-generic interface with generic arguments
- [`KT-10237`](https://youtrack.jetbrains.com/issue/KT-10237) Exception on an unresolved symbol in a type parameter bound in the 'where' clause
- [`KT-11821`](https://youtrack.jetbrains.com/issue/KT-11821) Exception on incorrect number of generic arguments in a type parameter bound in the 'where' clause
- [`KT-12482`](https://youtrack.jetbrains.com/issue/KT-12482) Exception: Implementation doesn't have the most specific type, but none of the other overridden methods does either
- [`KT-12687`](https://youtrack.jetbrains.com/issue/KT-12687) Exception when 'data' modifier is applied to object
- [`KT-9620`](https://youtrack.jetbrains.com/issue/KT-9620) AssertionError in DescriptorResolver#checkBounds
- [`KT-3689`](https://youtrack.jetbrains.com/issue/KT-3689) IllegalAccess on a property with private setter of the subclass
- [`KT-6391`](https://youtrack.jetbrains.com/issue/KT-6391) Wrong warning for array casting (Array<Any?> to Array<Any>)
- [`KT-8596`](https://youtrack.jetbrains.com/issue/KT-8596) Exception when analyzing nested class constructor reference in an argument position
- [`KT-12982`](https://youtrack.jetbrains.com/issue/KT-12982) Incorrect type inference when accessing mutable protected property via reflection
- [`KT-13206`](https://youtrack.jetbrains.com/issue/KT-13206) Report "Cast never succeeds" if and only if ClassCastException can be predicted
- [`KT-12467`](https://youtrack.jetbrains.com/issue/KT-12467) IllegalStateException: Concrete fake override should have exactly one concrete super-declaration: []
- [`KT-13340`](https://youtrack.jetbrains.com/issue/KT-13340) Report "return is not allowed here" only on the return keyword, not the whole expression
- [`KT-2349`](https://youtrack.jetbrains.com/issue/KT-2349), [`KT-6054`](https://youtrack.jetbrains.com/issue/KT-6054) Report "uninitialized enum entry" if enum entry is referenced before its declaration
- [`KT-12809`](https://youtrack.jetbrains.com/issue/KT-12809) Report "uninitialized variable" if property is referenced before its declaration
- [`KT-260`](https://youtrack.jetbrains.com/issue/KT-260) Do not report "cast never succeeds" when casting nullable to nullable
- [`KT-11769`](https://youtrack.jetbrains.com/issue/KT-11769) Prohibit access from enum instance initialization code to members of enum's companion object
- [`KT-13371`](https://youtrack.jetbrains.com/issue/KT-13371) Fix CompilationException: Rewrite at slice LEAKING_THIS key: REFERENCE_EXPRESSION
- [`KT-13401`](https://youtrack.jetbrains.com/issue/KT-13401) Fix StackOverflowError when checking variance
- [`KT-13330`](https://youtrack.jetbrains.com/issue/KT-13330), [`KT-13349`](https://youtrack.jetbrains.com/issue/KT-13349) Fix AssertionError: Illegal resolved call to variable with invoke
- [`KT-13421`](https://youtrack.jetbrains.com/issue/KT-13421) Fix AssertionError: Only integer constants should be checked for overflow
- [`KT-13555`](https://youtrack.jetbrains.com/issue/KT-13555) Fix internal error "resolveToInstruction"
- [`KT-8989`](https://youtrack.jetbrains.com/issue/KT-8989) Change error messages: Replace "invisible_fake" with "invisible (private in a supertype)"
- [`KT-13612`](https://youtrack.jetbrains.com/issue/KT-13612) Val reassignment in try / catch
- [`KT-5469`](https://youtrack.jetbrains.com/issue/KT-5469) Incorrect "is never used" warning for value used in catch block
- [`KT-13510`](https://youtrack.jetbrains.com/issue/KT-13510) Missing "Nested class not allowed" error for anonymous object inside val initializer
- [`KT-13685`](https://youtrack.jetbrains.com/issue/KT-13685) Fix NPE when resolving callable references on incomplete code
- Change error messages: Fix quotes around keywords in diagnostic messages
- Change error messages: Remove quotes around visibilities

#### Parser

- [`KT-7118`](https://youtrack.jetbrains.com/issue/KT-7118) Improve error message after trailing dot in floating point literal
- [`KT-4948`](https://youtrack.jetbrains.com/issue/KT-4948) Recover by following keyword
- [`KT-7915`](https://youtrack.jetbrains.com/issue/KT-7915) Recover after val with no subsequent name
- [`KT-12987`](https://youtrack.jetbrains.com/issue/KT-12987) Recover after val with no name before declaration starting with soft keyword

#### JVM code generation

- [`KT-12909`](https://youtrack.jetbrains.com/issue/KT-12909) Do not generate redundant bridge for special built-in override
- [`KT-11915`](https://youtrack.jetbrains.com/issue/KT-11915) Exception in entrySet when Map implementation in Kotlin extends another one
- [`KT-12755`](https://youtrack.jetbrains.com/issue/KT-12755) Exception on property generation in multi-file classes
- [`KT-12983`](https://youtrack.jetbrains.com/issue/KT-12983) VerifyError: Bad type on operand stack in arraylength
- [`KT-12908`](https://youtrack.jetbrains.com/issue/KT-12908) Variable initialization in loop causes VerifyError: Bad local variable type
- [`KT-13040`](https://youtrack.jetbrains.com/issue/KT-13040) Invalid bytecode generated for extension lambda invocation with safe call
- [`KT-13023`](https://youtrack.jetbrains.com/issue/KT-13023) Char operations throw ClassCastException for boxed Chars
- [`KT-11634`](https://youtrack.jetbrains.com/issue/KT-11634) Exception for super call in delegation
- [`KT-12359`](https://youtrack.jetbrains.com/issue/KT-12359) Redundant stubs are generated on inheriting from java.util.Collection
- [`KT-11833`](https://youtrack.jetbrains.com/issue/KT-11833) Error generating constructors of class on anonymous object inheriting from nested class of super class
- [`KT-13133`](https://youtrack.jetbrains.com/issue/KT-13133) Incorrect InnerClasses attribute value for anonymous object copied from an inline function
- [`KT-13241`](https://youtrack.jetbrains.com/issue/KT-13241) Indices optimization leads to VerifyError with smart cast receiver
- [`KT-13374`](https://youtrack.jetbrains.com/issue/KT-13374) Fix compiler exception when inline function contains anonymous object implementing an interface by delegation

##### Generated code performance

- [`KT-11964`](https://youtrack.jetbrains.com/issue/KT-11964) No TABLESWITCH in when on enum bytecode if enum constant is imported
- [`KT-6916`](https://youtrack.jetbrains.com/issue/KT-6916) Optimize 'for' over 'downTo'
- [`KT-12733`](https://youtrack.jetbrains.com/issue/KT-12733) Optimize 'for' over 'rangeTo' as a non-qualified call

### Standard Library

- [`KT-13115`](https://youtrack.jetbrains.com/issue/KT-13115), [`KT-13297`](https://youtrack.jetbrains.com/issue/KT-13297) Improve documentation formatting, clarify documentation for `FileTreeWalk`, `Sequence` and `generateSequence`.
- [`KT-12894`](https://youtrack.jetbrains.com/issue/KT-12894) Do not fail in `Closeable.use` if the resource is `null`.

### Reflection

- [`KT-12915`](https://youtrack.jetbrains.com/issue/KT-12915) Runtime exception on callBy of JvmStatic function with default arguments
- [`KT-12967`](https://youtrack.jetbrains.com/issue/KT-12967) Runtime exception on reference to generic property
- [`KT-13370`](https://youtrack.jetbrains.com/issue/KT-13370) NullPointerException on companionObjectInstance of a built-in class
- [`KT-13462`](https://youtrack.jetbrains.com/issue/KT-13462) Make KClass for primitive type equal to the corresponding KClass for wrapper type

### IDE

- [`KT-12655`](https://youtrack.jetbrains.com/issue/KT-12655) New Kotlin file: extra error message for already existing file
- [`KT-12760`](https://youtrack.jetbrains.com/issue/KT-12760) Prohibit running non-Unit returning main function
- [`KT-12893`](https://youtrack.jetbrains.com/issue/KT-12893) Impossible to open Kotlin compiler settings
- [`KT-10433`](https://youtrack.jetbrains.com/issue/KT-10433) Copy-pasting reference to companion object member causes import dialog
- [`KT-12803`](https://youtrack.jetbrains.com/issue/KT-12803) Class is marked as unused when it is only used is in method reference
- [`KT-13084`](https://youtrack.jetbrains.com/issue/KT-13084) Run test method action executes all tests from same kotlin file
- [`KT-12718`](https://youtrack.jetbrains.com/issue/KT-12718) Deadlock due to index reentering
- [`KT-13114`](https://youtrack.jetbrains.com/issue/KT-13114) 'Unused declaration' option 'JUnit static methods' is always enabled
- [`KT-12997`](https://youtrack.jetbrains.com/issue/KT-12997) Override/Implement Members: Support "Copy JavaDoc" options for library classes
- [`KT-12887`](https://youtrack.jetbrains.com/issue/KT-12887) "Extend selection" should select call's invoked expression
- [`KT-13383`](https://youtrack.jetbrains.com/issue/KT-13383), [`KT-13379`](https://youtrack.jetbrains.com/issue/KT-13379) Override/Implement Members: Do not make return type non-nullable if base return type is explicitly nullable
- [`KT-13218`](https://youtrack.jetbrains.com/issue/KT-13218) Extract Function: Fix AssertionError on callable references
- [`KT-6520`](https://youtrack.jetbrains.com/issue/KT-6520) Introduce 'maino' and 'psvmo' templates for generating main in object
- [`KT-13455`](https://youtrack.jetbrains.com/issue/KT-13455) Override/Implement: Make return type non-nullable (platform collection case) when overriding Java method
- [`KT-10209`](https://youtrack.jetbrains.com/issue/KT-10209) Find Usages: Do not duplicate containing declaration in super member warning dialog
- [`KT-12977`](https://youtrack.jetbrains.com/issue/KT-12977) Hybrid dependency causes "outdated binary" warning to appear in non-js project
- [`KT-13057`](https://youtrack.jetbrains.com/issue/KT-13057) Go to inheritors on Enum should navigate to all enum classes
- Fix exception when choose Gradle configurer after project is synced
- Allow configuring Kotlin in Gradle module without Kotlin sources
- Show all Kotlin annotations when browsing hierarchy of "java.lang.Annotation"

#### Completion

- [`KT-12793`](https://youtrack.jetbrains.com/issue/KT-12793) Suggest abstract protected extension methods

#### Performance

- [`KT-12645`](https://youtrack.jetbrains.com/issue/KT-12645) Lazily calculate FQ name for local classes
- [`KT-13071`](https://youtrack.jetbrains.com/issue/KT-13071) Fix severe freezes because of long lint checks on large files

#### Highlighting

- [`KT-12937`](https://youtrack.jetbrains.com/issue/KT-12937) Java synthetic accessors highlighting does not differ from local variables

#### KDoc

- [`KT-12998`](https://youtrack.jetbrains.com/issue/KT-12998) Backslash is not rendered
- [`KT-12999`](https://youtrack.jetbrains.com/issue/KT-12999) Backtick inside inline code block is not rendered
- [`KT-13000`](https://youtrack.jetbrains.com/issue/KT-13000) Exclamation mark is not rendered
- [`KT-10398`](https://youtrack.jetbrains.com/issue/KT-10398) Fully qualified link is not resolved in editor
- [`KT-12932`](https://youtrack.jetbrains.com/issue/KT-12932) Link to library element is not clickable
- [`KT-10654`](https://youtrack.jetbrains.com/issue/KT-10654) Quick Doc can't follow KDoc link in referenced function description
- [`KT-9271`](https://youtrack.jetbrains.com/issue/KT-9271) Show Quick Doc for implicit lambda parameter 'it'

#### Formatter

- [`KT-12830`](https://youtrack.jetbrains.com/issue/KT-12830) Remove spaces before *?* in nullable types
- [`KT-13314`](https://youtrack.jetbrains.com/issue/KT-13314) Format spaces around !is and !in

#### Intention actions, inspections and quickfixes

##### New features

- [`KT-12152`](https://youtrack.jetbrains.com/issue/KT-12152) "Leaking this" inspection reports dangerous operations inside constructors including:

   * Accessing non-final property in constructor
   * Calling non-final function in constructor
   * Using 'this' as function argument in constructor of non-final class

- [`KT-13187`](https://youtrack.jetbrains.com/issue/KT-13187) "Make constructor parameter a val" should make the val private or public depending on its option
- [`KT-5771`](https://youtrack.jetbrains.com/issue/KT-5771) Mark setter parameter type as redundant and provide quickfix to remove it
- [`KT-9228`](https://youtrack.jetbrains.com/issue/KT-9228) Add quickfix to remove '@' from annotation used as argument of another annotation
- [`KT-12251`](https://youtrack.jetbrains.com/issue/KT-12251) Add quickfix to fix type mismatch for primitive literals
- [`KT-12838`](https://youtrack.jetbrains.com/issue/KT-12838) Add quickfix for "Illegal usage of inline parameter" that adds `noinline`
- [`KT-13134`](https://youtrack.jetbrains.com/issue/KT-13134) Add quickfix for wrong Long suffix (Use `L` instead of `l`)
- [`KT-10903`](https://youtrack.jetbrains.com/issue/KT-10903) Add intention to convert lambda to function reference
- [`KT-7492`](https://youtrack.jetbrains.com/issue/KT-7492) Support "Create abstract function/property" inside an abstract class
- [`KT-10668`](https://youtrack.jetbrains.com/issue/KT-10668) Support "Create member/extension" corresponding to the extension receiver of enclosing function
- [`KT-12553`](https://youtrack.jetbrains.com/issue/KT-12553) Show versions in inspection about different version of Kotlin plugin in Maven and IDE plugin
- [`KT-12489`](https://youtrack.jetbrains.com/issue/KT-12489) Implement intention to replace camel-case test function name with a space-separated one
- [`KT-12730`](https://youtrack.jetbrains.com/issue/KT-12730) Warn about using different versions of Kotlin Gradle plugin and bundled compiler
- [`KT-13173`](https://youtrack.jetbrains.com/issue/KT-13173) Handle more cases in "Add Const Modifier" Intention
- [`KT-12628`](https://youtrack.jetbrains.com/issue/KT-12628) Quickfix for `invoke` operator unsafe calls
- [`KT-11425`](https://youtrack.jetbrains.com/issue/KT-11425) Inspection and quickfix to replace usages of `equals()` and `compareTo()` with operators
- [`KT-13113`](https://youtrack.jetbrains.com/issue/KT-13113) Inspection to detect redundant string templates
- [`KT-13011`](https://youtrack.jetbrains.com/issue/KT-13011) Inspection and quickfix for unnecessary lateinit
- [`KT-10731`](https://youtrack.jetbrains.com/issue/KT-10731) Inspection and quickfix for unnecessary use of toString() inside string interpolation
- [`KT-12043`](https://youtrack.jetbrains.com/issue/KT-12043) Intention to add / remove braces for when entry/entries
- [`KT-13483`](https://youtrack.jetbrains.com/issue/KT-13483) Intention to replace `a..b-1` with `a until b` and vice versa
- [`KT-6975`](https://youtrack.jetbrains.com/issue/KT-6975) Quickfix for adding 'inline' to a function with reified generic

##### Bugfixes

- Show receiver type in the text of "Create extension" quick fix
- Show target class name in the text of "Create member" quick fix
- [`KT-12869`](https://youtrack.jetbrains.com/issue/KT-12869) Usages of overridden Java method through synthetic accessors are not found
- [`KT-12813`](https://youtrack.jetbrains.com/issue/KT-12813) "Find Usages" for property returns function calls
- [`KT-7722`](https://youtrack.jetbrains.com/issue/KT-7722) Approximate unresolvable types in "Create from Usage" quickfixes
- [`KT-11115`](https://youtrack.jetbrains.com/issue/KT-11115) Implement Members: Fix base member detection when abstract and non-abstract members with matching signatures are inherited from an interface
- [`KT-12876`](https://youtrack.jetbrains.com/issue/KT-12876) Bogus suggestion to move property to constructor
- [`KT-13055`](https://youtrack.jetbrains.com/issue/KT-13055) Exception in "Specify Type Explicitly" intention
- [`KT-12942`](https://youtrack.jetbrains.com/issue/KT-12942) "Replace 'when' with 'if'" intention changes semantics when 'if' statements are used
- [`KT-12646`](https://youtrack.jetbrains.com/issue/KT-12646) 'Convert to block body' should use partial body resolve
- [`KT-12919`](https://youtrack.jetbrains.com/issue/KT-12919) Use simple class name in "Change function return type" quickfix
- [`KT-13151`](https://youtrack.jetbrains.com/issue/KT-13151) Incorrect warning "Make variable immutable"
- [`KT-13170`](https://youtrack.jetbrains.com/issue/KT-13170) "Declaration has platform type" inspection: by default should not be reported for platform type arguments
- [`KT-13262`](https://youtrack.jetbrains.com/issue/KT-13262) "Wrap with safe let call" quickfix produces wrong result for qualified function
- [`KT-13364`](https://youtrack.jetbrains.com/issue/KT-13364) Do not suggest creating annotations/enum classes for unresolved type parameter bounds
- [`KT-12627`](https://youtrack.jetbrains.com/issue/KT-12627) Allow warnings suppression for secondary constructor
- [`KT-13365`](https://youtrack.jetbrains.com/issue/KT-13365) Disable "Create property" (non-abstract) in interfaces. Make "Create function" (non-abstract) generate function body in interfaces
- [`KT-8903`](https://youtrack.jetbrains.com/issue/KT-8903) Remove Unused Receiver: update function/property usages
- [`KT-11799`](https://youtrack.jetbrains.com/issue/KT-11799) Create from Usage: Make extension functions/properties 'private' by default
- [`KT-11795`](https://youtrack.jetbrains.com/issue/KT-11795) Create from Usage: Place extension properties after the usage and generate stub getter
- [`KT-12951`](https://youtrack.jetbrains.com/issue/KT-12951) Prohibit "Convert to expression body" when function body is 'if' without 'else' or 'when' is non-exhaustive
- [`KT-13430`](https://youtrack.jetbrains.com/issue/KT-13430) "Add non-null asserted (!!) call" quickfix can't process unary operators
- [`KT-13336`](https://youtrack.jetbrains.com/issue/KT-13336) "Convert concatenation to template" intention appends literal to variable omitting braces
- [`KT-13328`](https://youtrack.jetbrains.com/issue/KT-13328) Do not suggest "Replace infix with safe call" inside conditions or binary / unary expressions
- [`KT-13452`](https://youtrack.jetbrains.com/issue/KT-13452) "Replace if expression with assignment" doesn't work for cascade if-else if-else
- [`KT-13184`](https://youtrack.jetbrains.com/issue/KT-13184) "Different Kotlin Version" inspection: false positive caused by verbose plugin version name
- [`KT-13480`](https://youtrack.jetbrains.com/issue/KT-13480) "Can be replaced with comparison" inspection: false positive if extension method called 'equals' is used
- [`KT-13288`](https://youtrack.jetbrains.com/issue/KT-13288) "Unused property" inspection: false positive when extending abstract class and implementing interface
- [`KT-13432`](https://youtrack.jetbrains.com/issue/KT-13432) "Replace with safe call" quickfix does not work with `compareTo()` usage
- [`KT-13444`](https://youtrack.jetbrains.com/issue/KT-13444) "Invert if" intention changes semantics for nested if with return
- [`KT-13536`](https://youtrack.jetbrains.com/issue/KT-13536) Fix StackOverflowError from "Unused Symbol" inspection after importing enum's values()
- [`KT-12820`](https://youtrack.jetbrains.com/issue/KT-12820) Platform Type Inspection: !! quickfix shouldn't be available when any generic parameter has platform type
- [`KT-9825`](https://youtrack.jetbrains.com/issue/KT-9825) Incorrect "unused variable" warning when used in finally block
- [`KT-13715`](https://youtrack.jetbrains.com/issue/KT-13715) Prohibit applying "Change to star projection" to functional types

#### Refactorings

##### New features

- [`KT-12017`](https://youtrack.jetbrains.com/issue/KT-12017) Inline Property: Support "Do not show this dialog" and "Inline this occurrence" options

##### Bugfixes

- [`KT-11176`](https://youtrack.jetbrains.com/issue/KT-11176) Add a space before '{' in functions generated "Generate hashCode/equals/toString"
- [`KT-12294`](https://youtrack.jetbrains.com/issue/KT-12294) Introduce Property: Fix extraction of expressions referring to primary constructor parameters
- [`KT-12413`](https://youtrack.jetbrains.com/issue/KT-12413) Change Signature: Fix bogus warning about unresolved type parameters/invalid functional type replacement
- [`KT-12084`](https://youtrack.jetbrains.com/issue/KT-12084) Introduce Property: Do not skip outer classes if extractable expression is contained in object literal
- [`KT-13082`](https://youtrack.jetbrains.com/issue/KT-13082) Rename: Fix exception on property rename preview
- [`KT-13207`](https://youtrack.jetbrains.com/issue/KT-13207) Safe delete: Fix exception when removing any function in 2016.2
- [`KT-12945`](https://youtrack.jetbrains.com/issue/KT-12945) Rename: Fix function description in super method warning dialog
- [`KT-12922`](https://youtrack.jetbrains.com/issue/KT-12922) Introduce Variable: Do not suggest expressions without type
- [`KT-12943`](https://youtrack.jetbrains.com/issue/KT-12943) Rename: Show function signatures in "Rename Overloads" dialog
- [`KT-13157`](https://youtrack.jetbrains.com/issue/KT-13157) Extract Function: Automatically quote function name if necessary
- [`KT-13010`](https://youtrack.jetbrains.com/issue/KT-13010) Extract Function: Fix generation of destructuring declarations
- [`KT-13128`](https://youtrack.jetbrains.com/issue/KT-13128) Introduce Variable: Retain entered name after changing "Specify type explicitly" option
- [`KT-13054`](https://youtrack.jetbrains.com/issue/KT-13054) Introduce Variable: Skip leading/trailing comments inside selection
- [`KT-13385`](https://youtrack.jetbrains.com/issue/KT-13385) Move: Quote package name (if necessary) when moving declarations to new file
- [`KT-13395`](https://youtrack.jetbrains.com/issue/KT-13395) Introduce Property: Fix duplicate count in popup window
- [`KT-13277`](https://youtrack.jetbrains.com/issue/KT-13277) Change Signature: Fix usage processing to prevent interfering with Python support plugin
- [`KT-13254`](https://youtrack.jetbrains.com/issue/KT-13254) Rename: Conflict detection for type parameters
- [`KT-13282`](https://youtrack.jetbrains.com/issue/KT-13282), [`KT-13283`](https://youtrack.jetbrains.com/issue/KT-13283) Rename: Fix name quoting for automatic renamers
- [`KT-13239`](https://youtrack.jetbrains.com/issue/KT-13239) Rename: Warn about function name conflicts
- [`KT-13174`](https://youtrack.jetbrains.com/issue/KT-13174) Move: Warn about accessibility conflicts due to moving to unrelated module
- [`KT-13175`](https://youtrack.jetbrains.com/issue/KT-13175) Move: Warn about accessibility conflicts when moving entire file
- [`KT-13240`](https://youtrack.jetbrains.com/issue/KT-13240) Rename: Do not report shadowing conflict if redeclaration is detected
- [`KT-13253`](https://youtrack.jetbrains.com/issue/KT-13253) Rename: Report conflicts for constructor parameters
- [`KT-12971`](https://youtrack.jetbrains.com/issue/KT-12971) Push Down: Do not specifiy visibility on generated overriding members
- [`KT-13124`](https://youtrack.jetbrains.com/issue/KT-13124) Pull Up: Skip super members without explicit declarations
- [`KT-13032`](https://youtrack.jetbrains.com/issue/KT-13032) Rename: Support accessors with non-conventional names
- [`KT-13463`](https://youtrack.jetbrains.com/issue/KT-13463) Rename: Quote parameter name when necessary
- [`KT-13476`](https://youtrack.jetbrains.com/issue/KT-13476) Rename: Fix parameter rename when new name matches call selector
- [`KT-9381`](https://youtrack.jetbrains.com/issue/KT-9381) Rename: Do not search for component convention usages
- [`KT-13488`](https://youtrack.jetbrains.com/issue/KT-13488) Rename: Support rename of packages with non-standard quoted names

#### Debugger

##### New features

- [`KT-7549`](https://youtrack.jetbrains.com/issue/KT-7549) Provide an option to use the Kotlin syntax when evaluating watches and expressions in Java files

##### Bugfixes

- [`KT-13059`](https://youtrack.jetbrains.com/issue/KT-13059) Fix error stepping on *Step Over* action in the end of while block
- [`KT-13037`](https://youtrack.jetbrains.com/issue/KT-13037) Fix possible deadlock in debugger in 2016.1 and exception in 2016.2
- [`KT-12651`](https://youtrack.jetbrains.com/issue/KT-12651) Fix exception in evaluate expression when bad identifier is used for marking object
- [`KT-12896`](https://youtrack.jetbrains.com/issue/KT-12896) Fix "Step In" to inline functions for Android
- [`KT-13269`](https://youtrack.jetbrains.com/issue/KT-13269) Make quick evaluate work on receiver in qualified expressions
- [`KT-12641`](https://youtrack.jetbrains.com/issue/KT-12641) Unknown error on evaluate expression containing inline functions with complicated environment
- [`KT-13163`](https://youtrack.jetbrains.com/issue/KT-13163) Fix exception when evaluating expression: Access is allowed from event dispatch thread only.

### JS

#### New features

- [`KT-3008`](https://youtrack.jetbrains.com/issue/KT-3008) Option to generate require.js and AMD compatible modules
- [`KT-5987`](https://youtrack.jetbrains.com/issue/KT-5987) Add ability to refer to class
- [`KT-4115`](https://youtrack.jetbrains.com/issue/KT-4115) Provide method to get Kotlin type name

#### Bugfixes

- [`KT-8003`](https://youtrack.jetbrains.com/issue/KT-8003) Compiler exception on 'throw throw'
- [`KT-8318`](https://youtrack.jetbrains.com/issue/KT-8318) Wrong result for 'when' containing only 'else' block
- [`KT-12157`](https://youtrack.jetbrains.com/issue/KT-12157) Compiler exception on `when` condition containing `return`, `break` or `continue`
- [`KT-12275`](https://youtrack.jetbrains.com/issue/KT-12275) Fix code generation with inline function call in condition of `while`/`do..while`
- [`KT-13160`](https://youtrack.jetbrains.com/issue/KT-13160) Fix compiler exception when left-hand side of assignment is array access and right-hand side is inline function
- [`KT-12864`](https://youtrack.jetbrains.com/issue/KT-12864) Make enums comparable
- [`KT-12865`](https://youtrack.jetbrains.com/issue/KT-12865) Implementing Comparable breaks inheritance
- [`KT-12928`](https://youtrack.jetbrains.com/issue/KT-12928) Nested inline causes undefined reference access
- [`KT-12929`](https://youtrack.jetbrains.com/issue/KT-12929) Code with callable reference crashed at runtime (in some JS VMs)
- [`KT-13043`](https://youtrack.jetbrains.com/issue/KT-13043) Invalid invocation generated for secondary constructor that calls constructor from base class with default parameters
- [`KT-13025`](https://youtrack.jetbrains.com/issue/KT-13025) 'function?.invoke' does not work properly with extension functions
- [`KT-12807`](https://youtrack.jetbrains.com/issue/KT-12807) Lambda was lost in generated code
- [`KT-12808`](https://youtrack.jetbrains.com/issue/KT-12808) Compiler duplicates arguments and the body of lambda when lambda is in RHS of assignment operator
- [`KT-12873`](https://youtrack.jetbrains.com/issue/KT-12873) Fix ReferenceError when class delegates to complex expression
- [`KT-13658`](https://youtrack.jetbrains.com/issue/KT-13658) Wrong code when capturing object


### Tools

#### Gradle

- Gradle versions < 2.0 are not supported
- [`KT-13234`](https://youtrack.jetbrains.com/issue/KT-13234) Setting kotlinOptions.destination and kotlinOptions.classpath is deprecated
- [`KT-9392`](https://youtrack.jetbrains.com/issue/KT-9392) Kotlin classes are missing after converting Java class to Kotlin
- [`KT-12736`](https://youtrack.jetbrains.com/issue/KT-12736) Kotlin classes are deleted when generated Java source is changed
- [`KT-12658`](https://youtrack.jetbrains.com/issue/KT-12658) Build fails after android resources are edited
- [`KT-12750`](https://youtrack.jetbrains.com/issue/KT-12750) Non clean compilation fails with gradle 2.14
- [`KT-12912`](https://youtrack.jetbrains.com/issue/KT-12912) New class from subproject is unresolved with subsequent build with Gradle Daemon
- [`KT-12962`](https://youtrack.jetbrains.com/issue/KT-12962) Incremental compilation: Track changes in generated files
- [`KT-12923`](https://youtrack.jetbrains.com/issue/KT-12923) Incremental compilation: Compile error when code using internal class is modified
- [`KT-13528`](https://youtrack.jetbrains.com/issue/KT-13528) Incremental compilation: support multi-project incremental compilation
- [`KT-13732`](https://youtrack.jetbrains.com/issue/KT-13732) Android Build folder littered with `copyFlavourTypeXXX`

#### KAPT

##### New features

- [`KT-13499`](https://youtrack.jetbrains.com/issue/KT-13499) Implement Annotation Processing API (JSR 269) natively in Kotlin

##### Bugfixes

- [`KT-12776`](https://youtrack.jetbrains.com/issue/KT-12776) Android build fails with KAPT and generateStubs depending on library module names
- [`KT-13179`](https://youtrack.jetbrains.com/issue/KT-13179) Java is recompiled every time with Gradle 2.14 and KAPT
- [`KT-12303`](https://youtrack.jetbrains.com/issue/KT-12303), [`KT-12113`](https://youtrack.jetbrains.com/issue/KT-12113) Do not pass non-relevant annotations to processors

#### REPL

- [`KT-12389`](https://youtrack.jetbrains.com/issue/KT-12389) REPL just quits when toString() of user class throws an exception

#### CLI & Ant

- [`KT-13237`](https://youtrack.jetbrains.com/issue/KT-13237) Include kotlin-reflect.jar to classpath by default, add '-no-reflect' key to suppress this behavior

#### CLI

- [`KT-13491`](https://youtrack.jetbrains.com/issue/KT-13491) Support '-no-reflect' in 'kotlin' command

#### Maven

- [`KT-13211`](https://youtrack.jetbrains.com/issue/KT-13211) Provide better compilation failure info for TeamCity builds

#### Compiler daemon

- Fix exception "java.lang.NoClassDefFoundError: Could not initialize class kotlin.Unit"

## 1.0.3

### Compiler

#### Analysis & diagnostics

- Combination of `open` and `override` is no longer a warning
- [`KT-4829`](https://youtrack.jetbrains.com/issue/KT-4829) Equal conditions in `when` is now a warning
- [`KT-6611`](https://youtrack.jetbrains.com/issue/KT-6611) "This cast can never succeed" warning is no longer reported for `Foo<T> as Foo<Any>`
- [`KT-7174`](https://youtrack.jetbrains.com/issue/KT-7174) Declaring members with the same signature as non-overridable methods from Java classes (like Object.wait/notify) is now an error (when targeting JVM)
- [`KT-12302`](https://youtrack.jetbrains.com/issue/KT-12302) `abstract` modifier for a member of interface is no longer a warning
- [`KT-12452`](https://youtrack.jetbrains.com/issue/KT-12452) `open` modifier for a member of interface without implementation is now a warning
- [`KT-11111`](https://youtrack.jetbrains.com/issue/KT-11111) Overriding by inline function is now a warning, overriding by a function with reified type parameter is an error
- [`KT-12337`](https://youtrack.jetbrains.com/issue/KT-12337) Reference to a property with invisible setter now has KProperty type (as opposed to KMutableProperty)

###### Issues fixed

- [`KT-4285`](https://youtrack.jetbrains.com/issue/KT-4285) No warning for a non-tail call when the method inherits default arguments from superclass
- [`KT-4764`](https://youtrack.jetbrains.com/issue/KT-4764) Spurious "Variable must be initialized" in try/catch/finally
- [`KT-6665`](https://youtrack.jetbrains.com/issue/KT-6665) Unresolved reference leads to marking subsequent code unreachable
- [`KT-11750`](https://youtrack.jetbrains.com/issue/KT-11750) Exceptions when creating various entries with the name "name" in enums
- [`KT-11998`](https://youtrack.jetbrains.com/issue/KT-11998) Smart cast to not-null is not performed on a boolean property in `if` condition
- [`KT-10648`](https://youtrack.jetbrains.com/issue/KT-10648) Exhaustiveness check does not work when sealed class hierarchy contains intermediate sealed classes
- [`KT-10717`](https://youtrack.jetbrains.com/issue/KT-10717) Type inference for lambda with local return
- [`KT-11266`](https://youtrack.jetbrains.com/issue/KT-11266) Fixed "Empty intersection of types" internal compiler error for some cases
- [`KT-11857`](https://youtrack.jetbrains.com/issue/KT-11857) Fix visibility check for dynamic members within protected method (when targeting JS)
- [`KT-12589`](https://youtrack.jetbrains.com/issue/KT-12589) Improved "`infix` modifier is inapplicable" diagnostic message
- [`KT-11679`](https://youtrack.jetbrains.com/issue/KT-11679) Erroneous call with argument causes Throwable at ResolvedCallImpl.getArgumentMapping()
- [`KT-12623`](https://youtrack.jetbrains.com/issue/KT-12623) Fix ISE on malformed code

#### JVM code generation

- [`KT-5075`](https://youtrack.jetbrains.com/issue/KT-5075) Optimize array/collection indices usage in `for` loop
- [`KT-11116`](https://youtrack.jetbrains.com/issue/KT-11116) Optimize coercion to Unit, POP operations are backward-propagated

###### Issues fixed
- [`KT-11499`](https://youtrack.jetbrains.com/issue/KT-11499) Compiler crashes with "Incompatible stack heights"
- [`KT-11943`](https://youtrack.jetbrains.com/issue/KT-11943) CompilationException with extension property of KClass
- [`KT-12125`](https://youtrack.jetbrains.com/issue/KT-12125) Wrong increment/decrement on Byte/Char/Short.MAX_VALUE/MIN_VALUE
- [`KT-12192`](https://youtrack.jetbrains.com/issue/KT-12192) Exhaustiveness check isn't generated for when expression returning Unit
- [`KT-12200`](https://youtrack.jetbrains.com/issue/KT-12200) Erroneously optimized away assignment to a property initialized to zero
- [`KT-12582`](https://youtrack.jetbrains.com/issue/KT-12582) "VerifyError: Bad local variable type" caused by explicit loop variable type
- [`KT-12708`](https://youtrack.jetbrains.com/issue/KT-12708) Bridge method not generated when data class implements interface with copy() method
- [`KT-12106`](https://youtrack.jetbrains.com/issue/KT-12106) import static of reified companion object method throws IllegalAccessError

#### Performance

- Reduced number of IO operation when loading kotlin compiled classes

#### Сompiler options

- Allow to specify version of Kotlin language for source compatibility with older releases.
    - CLI: `-language-version` command line option
    - Maven: `languageVersion` configuration parameter, linked with `kotlin.compiler.languageVersion` property
    - Gradle: `kotlinOptions.languageVersion` property in task configuration
- Allow to specify which java runtime target version to generate bytecode for.
    - CLI: `-jvm-target` command line option
    - Maven: `jvmTarget` configuration parameter, linked with `kotlin.compiler.jvmTarget` property
    - Gradle: `kotlinOptions.jvmTarget` property in task configuration
- Allow to specify path to JDK to resolve classes from.
    - CLI: `-jdk-home` command line option
    - Maven: `jdkHome` configuration parameter, linked with `kotlin.compiler.jdkHome` property
    - Gradle: `kotlinOptions.jdkHome` property in task configuration

### Standard Library

- Improve documentation (including [`KT-11632`](https://youtrack.jetbrains.com/issue/KT-11632))
- List iteration used in collection operations is performed with an indexed loop when the list supports `RandomAccess` and the operation isn't inlined

### IDE

#### Completion

###### New features

- Smart completion after `by` and `in`
- Improved completion in bodies of overridden members (when no type is specified)
- Improved presentation of completion items for property accessors
- Fixed keyword completion after `try` in assignment expression
- [`KT-8527`](https://youtrack.jetbrains.com/issue/KT-8527) Include non-imported declarations on the first completion
- [`KT-12068`](https://youtrack.jetbrains.com/issue/KT-12068) Special completion item for "[]" get-operator access
- [`KT-12080`](https://youtrack.jetbrains.com/issue/KT-12080) Parameter names are now higher up in completion list

###### Issues fixed
- Fixed enum members being present in completion as static members
- Fixed QuickDoc not working for properties generated for java classes
- [`KT-9166`](https://youtrack.jetbrains.com/issue/KT-9166) Code completion does not work for synthetic java properties on typing "g"
- [`KT-11609`](https://youtrack.jetbrains.com/issue/KT-11609) No named arguments completion should be after dot
- [`KT-11633`](https://youtrack.jetbrains.com/issue/KT-11633) Wrong indentation after completing a statement in data class
- [`KT-11680`](https://youtrack.jetbrains.com/issue/KT-11680) Code completion of label for existing return with value inserts redundant whitespace
- [`KT-11784`](https://youtrack.jetbrains.com/issue/KT-11784) Completion for `if` statement should add parentheses automatically
- [`KT-11890`](https://youtrack.jetbrains.com/issue/KT-11890) Completion for callable references does not propose static Java members
- [`KT-11912`](https://youtrack.jetbrains.com/issue/KT-11912) String interpolation is not converted to ${} form when accessing this.property
- [`KT-11957`](https://youtrack.jetbrains.com/issue/KT-11957) No `catch` and `finally` keywords in completion
- [`KT-12103`](https://youtrack.jetbrains.com/issue/KT-12103) Smart completion for nested SAM-adapter produces short unresolved name
- [`KT-12138`](https://youtrack.jetbrains.com/issue/KT-12138) Do not show "::error" in smart completion when any function type accepting one argument is expected
- [`KT-12150`](https://youtrack.jetbrains.com/issue/KT-12150) Smart completion suggests to compare non-nullable with null
- [`KT-12124`](https://youtrack.jetbrains.com/issue/KT-12124) No code completion for a java property in a specific position
- [`KT-12299`](https://youtrack.jetbrains.com/issue/KT-12299) Completion: incorrect priority of property foo over method getFoo in Kotlin-only code
- [`KT-12328`](https://youtrack.jetbrains.com/issue/KT-12328) Qualified function name inserted when typing before `if`
- [`KT-12427`](https://youtrack.jetbrains.com/issue/KT-12427) Completion doesn't work for "@receiver:" annotation target
- [`KT-12447`](https://youtrack.jetbrains.com/issue/KT-12447) Don't use CompletionProgressIndicator in Kotlin plugin
- [`KT-12669`](https://youtrack.jetbrains.com/issue/KT-12669) Completion should show variant with `()` when there is default lambda
- [`KT-12369`](https://youtrack.jetbrains.com/issue/KT-12369) Pressing dot after class name should not cause insertion of constructor call

#### Spring support

###### New features

- [`KT-11692`](https://youtrack.jetbrains.com/issue/KT-11692) Support Spring model diagrams for Kotlin classes
- [`KT-12079`](https://youtrack.jetbrains.com/issue/KT-12079) Support "Autowired members defined in invalid Spring bean" inspection on Kotlin declarations
- [`KT-12092`](https://youtrack.jetbrains.com/issue/KT-12092) Implement bean references in @Qualifier annotations
- [`KT-12135`](https://youtrack.jetbrains.com/issue/KT-12135) Automatically configure components based on `basePackageClasses` attribute of @ComponentScan
- [`KT-12136`](https://youtrack.jetbrains.com/issue/KT-12136) Implement package references inside of string literals
- [`KT-12139`](https://youtrack.jetbrains.com/issue/KT-12139) Support Spring configurations linked via @Import annotation
- [`KT-12278`](https://youtrack.jetbrains.com/issue/KT-12278) Implement Spring @Autowired inspection
- [`KT-12465`](https://youtrack.jetbrains.com/issue/KT-12465) Implement Spring @ComponentScan inspection

###### Issues fixed

- [`KT-12091`](https://youtrack.jetbrains.com/issue/KT-12091) Fixed unstable behavior of Spring line markers
- [`KT-12096`](https://youtrack.jetbrains.com/issue/KT-12096) Fixed rename of custom-named beans specified with Kotlin annotation
- [`KT-12117`](https://youtrack.jetbrains.com/issue/KT-12117) Group Kotlin classes from the same file in the Choose Bean dialog
- [`KT-12120`](https://youtrack.jetbrains.com/issue/KT-12120) Show autowiring candidates line markers for @Autowired-annotated constructors and constructor parameters
- [`KT-12122`](https://youtrack.jetbrains.com/issue/KT-12122) Fixed line marker popup on functions with @Qualifier-annotated parameters
- [`KT-12143`](https://youtrack.jetbrains.com/issue/KT-12143) Fixed "Spring Facet Code Configuration (Kotlin)" inspection description
- [`KT-12147`](https://youtrack.jetbrains.com/issue/KT-12147) Fixed exception on analyzing object declaration with @Component annotation
- [`KT-12148`](https://youtrack.jetbrains.com/issue/KT-12148) Warn about object declarations annotated with Spring `@Configuration`/`@Component`/etc.
- [`KT-12363`](https://youtrack.jetbrains.com/issue/KT-12363) Fixed "Autowired members defined in invalid Spring bean (Kotlin)" inspection description
- [`KT-12366`](https://youtrack.jetbrains.com/issue/KT-12366) Fixed exception on analyzing class declaration upon annotation typing
- [`KT-12384`](https://youtrack.jetbrains.com/issue/KT-12384) Fixed bean references in factory method calls

#### Intention actions, inspections and quickfixes

###### New features

- New icon for "New -> Kotlin Activity" action
- "Change visibility on exposure" and "Make visible" fixes now support all possible visibilities
- [`KT-8477`](https://youtrack.jetbrains.com/issue/KT-8477) New inspection "Can be primary constructor property" with quick-fix
- [`KT-5010`](https://youtrack.jetbrains.com/issue/KT-5010) "Redundant semicolon" inspection with quickfix
- [`KT-9757`](https://youtrack.jetbrains.com/issue/KT-9757) Quickfix for "Unused lambda expression" warning
- [`KT-10844`](https://youtrack.jetbrains.com/issue/KT-10844) Quick fix to add crossinline modifier
- [`KT-11090`](https://youtrack.jetbrains.com/issue/KT-11090) "Add variance modifiers to type parameters" inspection
- [`KT-11255`](https://youtrack.jetbrains.com/issue/KT-11255) Move Element Left/Right actions
- [`KT-11450`](https://youtrack.jetbrains.com/issue/KT-11450) "Modality is redundant" inspection
- [`KT-11523`](https://youtrack.jetbrains.com/issue/KT-11523) "Add @JvmOverloads annotation" intention
- [`KT-11768`](https://youtrack.jetbrains.com/issue/KT-11768) "Introduce local variable" intention
- [`KT-11806`](https://youtrack.jetbrains.com/issue/KT-11806) Quick-fix to increase visibility for invisible member
- [`KT-11807`](https://youtrack.jetbrains.com/issue/KT-11807) Use function body template when generating overriding functions with default body
- [`KT-11864`](https://youtrack.jetbrains.com/issue/KT-11864) Suggest "Create function/secondary constructor" quick fix on argument type mismatch
- [`KT-11876`](https://youtrack.jetbrains.com/issue/KT-11876) Quickfix for "Extension function type is not allowed as supertype" error
- [`KT-11920`](https://youtrack.jetbrains.com/issue/KT-11920) "Increase visibility" and "Decrease visibility" quickfixes for exposed visibility errors
- [`KT-12089`](https://youtrack.jetbrains.com/issue/KT-12089) Quickfix "Make primary constructor parameter a property"
- [`KT-12121`](https://youtrack.jetbrains.com/issue/KT-12121) "Add `toString()` call" quickfix
- [`KT-11104`](https://youtrack.jetbrains.com/issue/KT-11104) New quickfixes for nullability problems: "Surround with null check" and "Wrap with safe let call"
- [`KT-12310`](https://youtrack.jetbrains.com/issue/KT-12310) New inspection "Member has platform type" with quickfix

###### Issues fixed

- Fixed "Convert property initializer getter" intention being available inside lambda initializer
- Improved message for "Can be declared as `val`" inspection
- [`KT-3797`](https://youtrack.jetbrains.com/issue/KT-3797) Quickfix to make a function abstract should not be offered for object members
- [`KT-11866`](https://youtrack.jetbrains.com/issue/KT-11866) Suggest "Create secondary constructor" when constructors exist but are not applicable
- [`KT-11482`](https://youtrack.jetbrains.com/issue/KT-11482) Fixed exception in "Move to companion object" intention
- [`KT-11483`](https://youtrack.jetbrains.com/issue/KT-11483) Pass implicit receiver as argument when moving member function to companion object
- [`KT-11512`](https://youtrack.jetbrains.com/issue/KT-11512) Allow choosing any source root in "Move file to directory" intention
- [`KT-10950`](https://youtrack.jetbrains.com/issue/KT-10950) Keep original file package name when moving top-level declarations to separate file (provided it's not ambiguous)
- [`KT-10174`](https://youtrack.jetbrains.com/issue/KT-10174) Optimize imports after applying "Move declaration to separate file" intention
- [`KT-11764`](https://youtrack.jetbrains.com/issue/KT-11764) Intention "Replace with a `forEach` function call should replace `continue` with `return@forEach`
- [`KT-11724`](https://youtrack.jetbrains.com/issue/KT-11724) False suggestion to replace with compound assignment
- [`KT-11805`](https://youtrack.jetbrains.com/issue/KT-11805) Invert if-condition intention breaks code in case of end of line comment
- [`KT-11811`](https://youtrack.jetbrains.com/issue/KT-11811) "Make protected" intention for a val declared in parameters of constructor
- [`KT-11710`](https://youtrack.jetbrains.com/issue/KT-11710) "Replace `if` with elvis operator": incorrect code generated for `if` expression
- [`KT-11849`](https://youtrack.jetbrains.com/issue/KT-11849) Replace explicit parameter with `it` changes the meaning of code because of the shadowing
- [`KT-11870`](https://youtrack.jetbrains.com/issue/KT-11870) "Replace with Elvis" refactoring doesn't change the variable type from T? to T
- [`KT-12069`](https://youtrack.jetbrains.com/issue/KT-12069) Specify language for all Kotlin code inspections
- [`KT-11366`](https://youtrack.jetbrains.com/issue/KT-11366) "object `Companion` is never used" warning in intellij
- [`KT-11275`](https://youtrack.jetbrains.com/issue/KT-11275) Inconsistent behaviour of "move lambda argument out of parentheses" intention action when using lambda calls with function arguments without parentheses
- [`KT-11594`](https://youtrack.jetbrains.com/issue/KT-11594) "Add non-null asserted (!!) call" applied to unsafe cast to nullable type causes AE at KtPsiFactory.createExpression()
- [`KT-11982`](https://youtrack.jetbrains.com/issue/KT-11982) False "Redundant final modifier" reported
- [`KT-12040`](https://youtrack.jetbrains.com/issue/KT-12040) "Replace when with if" produce invalid code for first entry which has comment
- [`KT-12204`](https://youtrack.jetbrains.com/issue/KT-12204) "Use classpath of module" option in existing Kotlin run configuration may be changed when a new run configuration is created
- [`KT-10635`](https://youtrack.jetbrains.com/issue/KT-10635) Don't mark private writeObject and readObject methods of Serializable classes as unused
- [`KT-11466`](https://youtrack.jetbrains.com/issue/KT-11466) "Make abstract" quick fix applies to outer class of object with accidentally abstract function
- [`KT-11120`](https://youtrack.jetbrains.com/issue/KT-11120) Constructor parameter/field reported as unused symbol even if it have `used` annotation
- [`KT-11974`](https://youtrack.jetbrains.com/issue/KT-11974) Invert if-condition intention loses comments
- [`KT-10812`](https://youtrack.jetbrains.com/issue/KT-10812) Globally unused constructors are not marked as such
- [`KT-11320`](https://youtrack.jetbrains.com/issue/KT-11320) Don't mark @BeforeClass (JUnit4) annotated functions as unused
- [`KT-12267`](https://youtrack.jetbrains.com/issue/KT-12267) "Change type" quick fix converts to Int for Long literal
- [`KT-11949`](https://youtrack.jetbrains.com/issue/KT-11949) Various problems fixed with "Constructor parameter is never used as a property" inspection
- [`KT-11716`](https://youtrack.jetbrains.com/issue/KT-11716) "Simply `for` using destructuring declaration" intention: incorrect behavior for data classes
- [`KT-12145`](https://youtrack.jetbrains.com/issue/KT-12145) "Simplify `for` using destructuring declaration" should work even when no variables declared inside loop
- [`KT-11933`](https://youtrack.jetbrains.com/issue/KT-11933) Entities used only by alias are marked as unused
- [`KT-12193`](https://youtrack.jetbrains.com/issue/KT-12193) Convert to block body isn't equivalent for when expressions returning Unit
- [`KT-10779`](https://youtrack.jetbrains.com/issue/KT-10779) Simplify `for` using destructing declaration: intention / inspection quick fix is available only when all variables are used
- [`KT-11281`](https://youtrack.jetbrains.com/issue/KT-11281) Fix exception on applying "Convert to class" intention to Java interface with Kotlin inheritor(s)
- [`KT-12285`](https://youtrack.jetbrains.com/issue/KT-12285) Fix exception on test class generation
- [`KT-12502`](https://youtrack.jetbrains.com/issue/KT-12502) Convert to expression body should be forbidden for non-exhaustive when returning Unit
- [`KT-12260`](https://youtrack.jetbrains.com/issue/KT-12260) ISE while replacing an operator with safe call
- [`KT-12649`](https://youtrack.jetbrains.com/issue/KT-12649) "Convert if to when" intention incorrectly deletes code
- [`KT-12671`](https://youtrack.jetbrains.com/issue/KT-12671) "Shot type" action: "Type is unknown" error on an invoked expression
- [`KT-12284`](https://youtrack.jetbrains.com/issue/KT-12284) Too wide applicability range for "Add braces to else" intention
- [`KT-11975`](https://youtrack.jetbrains.com/issue/KT-11975) "Invert if-condition" intention does not simplify `is` expression
- [`KT-12437`](https://youtrack.jetbrains.com/issue/KT-12437) "Replace explicit parameter" intention is suggested for parameter of inner lambda in presence of `it` from outer lambda
- [`KT-12290`](https://youtrack.jetbrains.com/issue/KT-12290) Navigate to the generated declaration when using "Implement abstract member" intention
- [`KT-12376`](https://youtrack.jetbrains.com/issue/KT-12376) Don't show "Package directive doesn't match file location" in injected code
- [`KT-12777`](https://youtrack.jetbrains.com/issue/KT-12777) Fix exception in "Create class" quickfix applied to unresolved references in type arguments

#### Language injection

- Apply injection for the literals in property initializer through property usages
- Enable injection from Java or Kotlin function declaration by annotating parameter with @Language annotation
- [`KT-2428`](https://youtrack.jetbrains.com/issue/KT-2428) Support basic use-cases of language injection for expressions marked with @Language annotation
- [`KT-11574`](https://youtrack.jetbrains.com/issue/KT-11574) Support predefined Java positions for language injection
- [`KT-11472`](https://youtrack.jetbrains.com/issue/KT-11472) Add comment or @Language annotation after "Inject language or reference" intention automatically

#### Refactorings

###### New features
- [`KT-6372`](https://youtrack.jetbrains.com/issue/KT-6372) Add name suggestions to Rename dialog
- [`KT-7851`](https://youtrack.jetbrains.com/issue/KT-7851) Respect naming conventions in automatic variable rename
- [`KT-8044`](https://youtrack.jetbrains.com/issue/KT-8044), [`KT-9432`](https://youtrack.jetbrains.com/issue/KT-9432) Support @JvmName annotation in rename refactoring
- [`KT-8512`](https://youtrack.jetbrains.com/issue/KT-8512) Support "Rename tests" options in Rename dialog
- [`KT-9168`](https://youtrack.jetbrains.com/issue/KT-9168) Support rename of synthetic properties
- [`KT-10578`](https://youtrack.jetbrains.com/issue/KT-10578) Support automatic test renaming for facade files
- [`KT-12657`](https://youtrack.jetbrains.com/issue/KT-12657) Rename implicit usages of annotation method `value`
- [`KT-12759`](https://youtrack.jetbrains.com/issue/KT-12759) Suggest renaming both property accessors with matching @JvmName when renaming one of them from Java

###### Issues fixed
- [`KT-4791`](https://youtrack.jetbrains.com/issue/KT-4791) Rename overridden property and all its accessors on attempt to rename overriding accessor in Java code
- [`KT-6363`](https://youtrack.jetbrains.com/issue/KT-6363) Do not rename ambiguous references in import directives
- [`KT-6663`](https://youtrack.jetbrains.com/issue/KT-6663) Fixed rename of ambiguous import reference to class/function when some referenced declarations are not changed
- [`KT-8510`](https://youtrack.jetbrains.com/issue/KT-8510) Preserve "Search in comments and strings" and "Search for text occurrences" settings in Rename dialog
- [`KT-8541`](https://youtrack.jetbrains.com/issue/KT-8541), [`KT-8786`](https://youtrack.jetbrains.com/issue/KT-8786) Do now show 'Rename overloads' options if target function has no overloads
- [`KT-8544`](https://youtrack.jetbrains.com/issue/KT-8544) Show more detailed description in Rename dialog
- [`KT-8562`](https://youtrack.jetbrains.com/issue/KT-8562) Show conflicts dialog on attempt of redeclaration
- [`KT-8611`](https://youtrack.jetbrains.com/issue/KT-8732) Qualify class references to resolve rename conflicts when possible
- [`KT-8732`](https://youtrack.jetbrains.com/issue/KT-8732) Implement Rename conflict analysis and fixes for properties/parameters
- [`KT-8860`](https://youtrack.jetbrains.com/issue/KT-8860) Allow renaming class by constructor delegation call referencing primary constructor
- [`KT-8892`](https://youtrack.jetbrains.com/issue/KT-8892) Suggest renaming base declarations on overriding members in object literals
- [`KT-9156`](https://youtrack.jetbrains.com/issue/KT-9156) Quote non-identifier names in Kotlin references
- [`KT-9157`](https://youtrack.jetbrains.com/issue/KT-9157) Fixed in-place rename of Kotlin expression referring Java declaration
- [`KT-9241`](https://youtrack.jetbrains.com/issue/KT-9241) Do not replace Java references to synthetic component functions when renaming constructor parameter
- [`KT-9435`](https://youtrack.jetbrains.com/issue/KT-9435) Process property accessor usages (Java) in comments and string literals
- [`KT-9444`](https://youtrack.jetbrains.com/issue/KT-9444) Rename dialog: Allow typing any identifier without backquotes
- [`KT-9446`](https://youtrack.jetbrains.com/issue/KT-9446) Copy default parameter values to overriding function which is renamed while its base function is not
- [`KT-9649`](https://youtrack.jetbrains.com/issue/KT-9649) Constraint search scope of parameter declared in a private member
- [`KT-10033`](https://youtrack.jetbrains.com/issue/KT-10033) Qualify references to members of enum companions in case of conflict with enum entries
- [`KT-10713`](https://youtrack.jetbrains.com/issue/KT-10713) Skip read-only declarations when renaming parameters
- [`KT-10687`](https://youtrack.jetbrains.com/issue/KT-10687) Qualify property references to avoid shadowing by parameters
- [`KT-11903`](https://youtrack.jetbrains.com/issue/KT-11903) Update references to facade class when renaming file via matching top-level class
- [`KT-12411`](https://youtrack.jetbrains.com/issue/KT-12411) Fix package name quotation in Move refactoring
- [`KT-12543`](https://youtrack.jetbrains.com/issue/KT-12543) Qualify property references with `this` to avoid renaming conflicts
- [`KT-12732`](https://youtrack.jetbrains.com/issue/KT-12732) Copy default parameter values to overriding function which is renamed by Java reference while its base function is unchanged
- [`KT-12747`](https://youtrack.jetbrains.com/issue/KT-12747) Fix exception on file copy

#### Java to Kotlin converter

###### New features

- [`KT-4727`](https://youtrack.jetbrains.com/issue/KT-4727) Convert Java code copied from browser or other sources

###### Issues fixed

- [`KT-11952`](https://youtrack.jetbrains.com/issue/KT-11952) Assertion failed in PropertyDetectionCache.get on conversion of access to Java constant of anonymous type
- [`KT-12046`](https://youtrack.jetbrains.com/issue/KT-12046) Recursive property setter
- [`KT-12039`](https://youtrack.jetbrains.com/issue/KT-12039) Static imports converted missing ".Companion"
- [`KT-12054`](https://youtrack.jetbrains.com/issue/KT-12054) Wrong conversion of `instanceof` checks with raw types
- [`KT-12045`](https://youtrack.jetbrains.com/issue/KT-12045) Convert `Object()` to `Any()`

#### Android Lint

###### Issues fixed

- [`KT-12015`](https://youtrack.jetbrains.com/issue/KT-12015) False positive for Bundle.getInt()
- [`KT-12023`](https://youtrack.jetbrains.com/issue/KT-12023) "minSdk" lint check doesn't work for `as`/`is`
- [`KT-12674`](https://youtrack.jetbrains.com/issue/KT-12674) "Calling new methods on older versions" errors for inlined constants
- [`KT-12681`](https://youtrack.jetbrains.com/issue/KT-12681) Running lint from main menu: diagnostics reported for java source files only
- [`KT-12173`](https://youtrack.jetbrains.com/issue/KT-12173) False positive for "Toast created but not shown" inside SAM adapter
- [`KT-12895`](https://youtrack.jetbrains.com/issue/KT-12895) NoSuchMethodError thrown when saving a Kotlin file

#### KDoc

###### New features
- Support for @receiver tag

###### Issues fixed
- Rendering of `_` and `*` standalone characters
- Rendering of code blocks
- [`KT-9933`](https://youtrack.jetbrains.com/issue/KT-9933) Indentation in code fragments is not preserved
- [`KT-10998`](https://youtrack.jetbrains.com/issue/KT-10998) Spaces around links are missing in return block
- [`KT-11791`](https://youtrack.jetbrains.com/issue/KT-11791) Markdown links rendering
- [`KT-12001`](https://youtrack.jetbrains.com/issue/KT-12001) Allow use of `@param` to document type parameter

#### Maven support

###### New features
- Inspections that check that kotlin IDEA plugin, kotlin Maven plugin and kotlin stdlib are of the same version
- [`KT-11643`](https://youtrack.jetbrains.com/issue/KT-11643) Inspections and intentions to fix erroneously configured Maven pom file
- [`KT-11701`](https://youtrack.jetbrains.com/issue/KT-11701) "Add Maven Dependency quick fix" in Kotlin source files
- [`KT-11743`](https://youtrack.jetbrains.com/issue/KT-11743) Intention to replace kotlin-test with kotlin-test-junit

###### Issues fixed
- [`KT-9492`](https://youtrack.jetbrains.com/issue/KT-9492) Configuring multiple Maven Modules
- [`KT-11642`](https://youtrack.jetbrains.com/issue/KT-11642) Kotlin Maven configurator tags order
- [`KT-11436`](https://youtrack.jetbrains.com/issue/KT-11436) "Choose Configurator" control opens dialogs with inconsistent modality (linux)
- [`KT-11731`](https://youtrack.jetbrains.com/issue/KT-11731) Default maven integration doesn't include documentation
- [`KT-12568`](https://youtrack.jetbrains.com/issue/KT-12568) Execution configuration: file path completion works only in some sub-elements of <sourceDirs>
- [`KT-12558`](https://youtrack.jetbrains.com/issue/KT-12558) Configure Kotlin in Project: "Undo" should revert changes in all poms
- [`KT-12512`](https://youtrack.jetbrains.com/issue/KT-12512) "Different IDE and Maven plugin version" inspection is being invoked for non-tracked pom.xml files

#### Debugger

###### New features
- [`KT-11438`](https://youtrack.jetbrains.com/issue/KT-11438) Support navigation from stacktrace to inline function call site

###### Issues fixed
- Do not step into inline lambda argument during step over inside inline function body
- Fix step over for inline argument with non-local return
- [`KT-12067`](https://youtrack.jetbrains.com/issue/KT-12067) Deadlock in Kotlin debugger is fixed
- [`KT-12232`](https://youtrack.jetbrains.com/issue/KT-12232) No code completion in Evaluate Expression and Throwable at CodeCompletionHandlerBase.invokeCompletion()
- [`KT-12137`](https://youtrack.jetbrains.com/issue/KT-12137) Evaluate expression: code completion/intention actions allows to use symbols from modules that are not referenced
- [`KT-12206`](https://youtrack.jetbrains.com/issue/KT-12206) NoSuchFieldError in Evaluate Expression on a property of a derived class
- [`KT-12678`](https://youtrack.jetbrains.com/issue/KT-12678) NoSuchFieldError in Evaluate Expression on accessing delegated property defined in other module
- [`KT-12773`](https://youtrack.jetbrains.com/issue/KT-12773) Fix debugging for Kotlin JS projects

#### Formatter

###### Issues fixed

- [`KT-12035`](https://youtrack.jetbrains.com/issue/KT-12035) Spaces around `as`
- [`KT-12018`](https://youtrack.jetbrains.com/issue/KT-12018) Spaces between function name and arguments in infix calls
- [`KT-11961`](https://youtrack.jetbrains.com/issue/KT-11961) Spaces before angle bracket in method definition
- [`KT-12175`](https://youtrack.jetbrains.com/issue/KT-12175) Don't enforce empty line between secondary constructors without body
- [`KT-12548`](https://youtrack.jetbrains.com/issue/KT-12548) Spaces around `is` keyword
- [`KT-12446`](https://youtrack.jetbrains.com/issue/KT-12446) Spaces before class type parameters
- [`KT-12634`](https://youtrack.jetbrains.com/issue/KT-12634) Spaces between method name and parenthesis in method call
- [`KT-10680`](https://youtrack.jetbrains.com/issue/KT-10680) Spaces around `in` keyword
- [`KT-12791`](https://youtrack.jetbrains.com/issue/KT-12791) Spaces between curly brace and expression inside string template
- [`KT-12781`](https://youtrack.jetbrains.com/issue/KT-12781) Spaces between annotation and expression
- [`KT-12689`](https://youtrack.jetbrains.com/issue/KT-12689) Spaces around semicolons
- [`KT-12714`](https://youtrack.jetbrains.com/issue/KT-12714) Spaces around parentheses in enum elements

#### Other

###### New features

- Added "Decompile" button to Kotlin bytecode toolwindow
- Added Kotlin "Tips of the day"
- Added "Kotlin 1.1 EAP" to "Configure Kotlin Plugin updates"
- [`KT-2919`](https://youtrack.jetbrains.com/issue/KT-2919) Constructor calls are no longer highlighted as classes
- [`KT-6540`](https://youtrack.jetbrains.com/issue/KT-6540) Infix function calls are now highlighted as regular function calls
- [`KT-9410`](https://youtrack.jetbrains.com/issue/KT-9410) Annotations in Kotlin are now highlighted with the same color as in Java by default
- [`KT-11465`](https://youtrack.jetbrains.com/issue/KT-11465) Type parameters in Kotlin are now highlighted with the same color as in Java by default
- [`KT-11657`](https://youtrack.jetbrains.com/issue/KT-11657) Allow viewing decompiled Java source code for Kotlin-compiled classes
- [`KT-11704`](https://youtrack.jetbrains.com/issue/KT-11704) Support file path references inside of Kotlin string literals
- [`KT-12076`](https://youtrack.jetbrains.com/issue/KT-12076) Kotlin Plugin update check: always display installed version number
- [`KT-11814`](https://youtrack.jetbrains.com/issue/KT-11814) New icon for kotlin annotation classes
- [`KT-12735`](https://youtrack.jetbrains.com/issue/KT-12735) Convert JavaDoc to KDoc when overriding Java class member in Kotlin

###### Issues fixed

- [`KT-5960`](https://youtrack.jetbrains.com/issue/KT-5960) Can't find usages for Java methods used from Kotlin by call convention
- [`KT-8362`](https://youtrack.jetbrains.com/issue/KT-8362) "New Kotlin file":  Keywords should be escaped in package name
- [`KT-8682`](https://youtrack.jetbrains.com/issue/KT-8682) Respect "Copy JavaDoc" option in the "Override/Implement Members..." dialog
- [`KT-8817`](https://youtrack.jetbrains.com/issue/KT-8817) Fixed rename of Java getters/setters through synthetic property references in Kotlin
- [`KT-9399`](https://youtrack.jetbrains.com/issue/KT-9399) Find Usages omits Kotlin annotation parameter usage in Java source
- [`KT-9797`](https://youtrack.jetbrains.com/issue/KT-9797) "Kotlin Bytecode" toolwindow breaks after closing
- [`KT-11145`](https://youtrack.jetbrains.com/issue/KT-11145) Use progress indicator when searching usages in Introduce Parameter
- [`KT-11155`](https://youtrack.jetbrains.com/issue/KT-11155) Allow running multiple Kotlin classes as well as running mixtures of Kotlin and Java classes
- [`KT-11495`](https://youtrack.jetbrains.com/issue/KT-11495) Show recursion line markers for extension function calls with different receiver
- [`KT-11659`](https://youtrack.jetbrains.com/issue/KT-11659) Generate abstract overrides for Any members inside of Kotlin interfaces
- [`KT-12070`](https://youtrack.jetbrains.com/issue/KT-12070) Add empty line in error message of Maven and Gradle configuration
- [`KT-11908`](https://youtrack.jetbrains.com/issue/KT-11908) Allow properties with custom setters to be used in generated equals/hashCode/toString
- [`KT-11617`](https://youtrack.jetbrains.com/issue/KT-11617) Fixed title of Introduce Parameter declaration chooser
- [`KT-11817`](https://youtrack.jetbrains.com/issue/KT-11817) Fixed rename of Kotlin enum constants through Java references
- [`KT-11816`](https://youtrack.jetbrains.com/issue/KT-11816) Fixed usages search for Safe Delete on simple enum entries
- [`KT-11282`](https://youtrack.jetbrains.com/issue/KT-11282) Delete interface reference from super-type list when applying Safe Delete to Java interface
- [`KT-11967`](https://youtrack.jetbrains.com/issue/KT-11967) Fix Find Usages/Rename for parameter references in XML files
- [`KT-10770`](https://youtrack.jetbrains.com/issue/KT-10770) "Optimize imports" will not keep import if a type is only referenced by kdoc
- [`KT-11955`](https://youtrack.jetbrains.com/issue/KT-11955) Copy/Paste inserts fully qualified name when copying function with overloads
- [`KT-12436`](https://youtrack.jetbrains.com/issue/KT-12436) "Replace explicit parameter with it": java.lang.Exception at BaseRefactoringProcessor.run()
- [`KT-12440`](https://youtrack.jetbrains.com/issue/KT-12440) Removing unused parameter results in Exception "Refactorings should not be started inside write action"
- [`KT-12006`](https://youtrack.jetbrains.com/issue/KT-12006) getLanguageLevel is slow for Kotlin light classes
- [`KT-12026`](https://youtrack.jetbrains.com/issue/KT-12026) "Constant expression required" in Java for const Kotlin values
- [`KT-12259`](https://youtrack.jetbrains.com/issue/KT-12259) ClassCastException in light classes while trying to create generic property
- [`KT-12289`](https://youtrack.jetbrains.com/issue/KT-12289) Remove unnecessary `?` from `serr` live template
- [`KT-12110`](https://youtrack.jetbrains.com/issue/KT-12110) Map help button of the Compiler - Kotlin page
- [`KT-12075`](https://youtrack.jetbrains.com/issue/KT-12075) Kotlin Plugin update check: make dumbaware
- [`KT-10255`](https://youtrack.jetbrains.com/issue/KT-10255) call BuildManager.clearState(project) in apply() method of Kotlin Compiler Settings configurable
- [`KT-11841`](https://youtrack.jetbrains.com/issue/KT-11841) New Project / Module wizard, Gradle: pure Kotlin module is created without `repositories` call in build.gradle
- [`KT-11095`](https://youtrack.jetbrains.com/issue/KT-11095) Java cannot infer generic return type of Kotlin function (with java 8 language level)
- [`KT-12090`](https://youtrack.jetbrains.com/issue/KT-12090) Intellij/Kotlin plugin does not handle generic return type of static method defined in Kotlin, called from Java
- [`KT-12206`](https://youtrack.jetbrains.com/issue/KT-12206) Fix NoSuchFieldError on accessing base property without backing field in evaluate expression
- [`KT-12516`](https://youtrack.jetbrains.com/issue/KT-12516) File Structure: Kotlin annotation classes have Java annotation icons
- [`KT-11328`](https://youtrack.jetbrains.com/issue/KT-11328) "New Kotlin class": generates packages when fully qualified name is specified
- [`KT-11778`](https://youtrack.jetbrains.com/issue/KT-11778) Exception in Lombok plugin: Rewrite at slice FUNCTION
- [`KT-11708`](https://youtrack.jetbrains.com/issue/KT-11708) "Go to declaration" doesn't work on a call to function with SAM conversion on a derived type
- [`KT-12381`](https://youtrack.jetbrains.com/issue/KT-12381) Prefer not-nullable return type when overriding Java method without nullability annotation
- [`KT-12647`](https://youtrack.jetbrains.com/issue/KT-12647) Performance improvement for test-related line markers
- [`KT-12526`](https://youtrack.jetbrains.com/issue/KT-12526) Kotlin intentions increase PSI modification counts from isAvailable, even in daemon threads

### Reflection

###### Issues fixed
- [`KT-11531`](https://youtrack.jetbrains.com/issue/KT-11531) Optimize "KCallable.name"
- [`KT-10771`](https://youtrack.jetbrains.com/issue/KT-10771) Reflection on Function objects does not support lambdas with generic return type
- [`KT-11824`](https://youtrack.jetbrains.com/issue/KT-11824) Reflection inconsistency between member property and accessor

### JS

- Improve performance of maps and sets

###### Issues fixed
- [`KT-6942`](https://youtrack.jetbrains.com/issue/KT-6942) Generate structural equality check (i.e. `Any.equals`) instead of referential check (===) value equality patterns in `when`
- [`KT-7228`](https://youtrack.jetbrains.com/issue/KT-7228) Wrong AbstractList signature
- [`KT-8299`](https://youtrack.jetbrains.com/issue/KT-8299) Wrong access to private member in autogenerated code in data class
- [`KT-11346`](https://youtrack.jetbrains.com/issue/KT-11346) Reified functions like `filterIsInstance` are now available in JS Standard Library
- [`KT-12305`](https://youtrack.jetbrains.com/issue/KT-12305) Incorrect translation of `vararg` in `@native` functions
- [`KT-12254`](https://youtrack.jetbrains.com/issue/KT-12254) JsEmptyExpression in initializer when compiling code like `val x = throw Exception()`
- [`KT-11960`](https://youtrack.jetbrains.com/issue/KT-11960) Wrong code generated when a method of a local class calls constructor of the class
- [`KT-10931`](https://youtrack.jetbrains.com/issue/KT-10931) Incorrect inlining of library method with optional parameters
- [`KT-12417`](https://youtrack.jetbrains.com/issue/KT-12417) Wrong check cast generated for KMutableProperty

### Tools

###### New features

- [`KT-11839`](https://youtrack.jetbrains.com/issue/KT-11839) Maven goal to execute kotlin script

###### Issues fixed

- KAPT: fix error when using enum constructors with parameters
- Various problems with gradle 2.2 fixed: [`KT-12478`](https://youtrack.jetbrains.com/issue/KT-12478), [`KT-12406`](https://youtrack.jetbrains.com/issue/KT-12406), [`KT-12478`](https://youtrack.jetbrains.com/issue/KT-12478)
- [`KT-12595`](https://youtrack.jetbrains.com/issue/KT-12595) JPS: Fixed com.intellij.util.io.MappingFailedException: Cannot map buffer
- [`KT-11166`](https://youtrack.jetbrains.com/issue/KT-11166) Gradle: Unable to access internal classes from test code within the same module
- [`KT-12352`](https://youtrack.jetbrains.com/issue/KT-12352) KAPT: Fix "Classpath entry points to a non-existent location" warnings
- [`KT-12074`](https://youtrack.jetbrains.com/issue/KT-12074) Building Kotlin maven projects using a parent pom will silently fail
- [`KT-11770`](https://youtrack.jetbrains.com/issue/KT-11770) Warning "RuntimeException: Could not find installation home path" when using Gradle Incremental Compilation
- [`KT-10969`](https://youtrack.jetbrains.com/issue/KT-10969) Android extensions: NullPointerException when finding view in Fragment
- [`KT-11885`](https://youtrack.jetbrains.com/issue/KT-11885) Gradle/Android: Unresolved reference "kotlinx" when classpath dependency is defined in root build.gradle
- [`KT-12786`](https://youtrack.jetbrains.com/issue/KT-12786) Deprecation warning with Gradle 2.14

## 1.0.2-1

- [KT-12159](https://youtrack.jetbrains.com/issue/KT-12159), [KT-12406](https://youtrack.jetbrains.com/issue/KT-12406), [KT-12431](https://youtrack.jetbrains.com/issue/KT-12431), [KT-12478](https://youtrack.jetbrains.com/issue/KT-12478) Support Android Studio 2.2
- [KT-11770](https://youtrack.jetbrains.com/issue/KT-11770) Fix warning "RuntimeException: Could not find installation home path" when using incremental compilation in Gradle
- [KT-12436](https://youtrack.jetbrains.com/issue/KT-12436), [KT-12440](https://youtrack.jetbrains.com/issue/KT-12440) Fix multiple exceptions during refactorings in IDEA 2016.2 EAP
- [KT-12015](https://youtrack.jetbrains.com/issue/KT-12015), [KT-12047](https://youtrack.jetbrains.com/issue/KT-12047), [KT-12387](https://youtrack.jetbrains.com/issue/KT-12387) Fix multiple issues in Kotlin Lint checks

## 1.0.2

### Compiler

#### Analysis & diagnostics

- [KT-7437](https://youtrack.jetbrains.com/issue/KT-7437), [KT-7971](https://youtrack.jetbrains.com/issue/KT-7971), [KT-7051](https://youtrack.jetbrains.com/issue/KT-7051), [KT-6125](https://youtrack.jetbrains.com/issue/KT-6125), [KT-6186](https://youtrack.jetbrains.com/issue/KT-6186), [KT-11649](https://youtrack.jetbrains.com/issue/KT-11649) Implement missing checks for protected visibility
- [KT-11666](https://youtrack.jetbrains.com/issue/KT-11666) Report "Implicit nothing return type" on non-override member functions
- [KT-4328](https://youtrack.jetbrains.com/issue/KT-4328), [KT-11497](https://youtrack.jetbrains.com/issue/KT-11497), [KT-10493](https://youtrack.jetbrains.com/issue/KT-10493), [KT-10820](https://youtrack.jetbrains.com/issue/KT-10820), [KT-11368](https://youtrack.jetbrains.com/issue/KT-11368) Report error if some classes were not found due to missing or conflicting dependencies
- [KT-11280](https://youtrack.jetbrains.com/issue/KT-11280) Do not perform smart casts for values with custom `equals` compared with `==`
- [KT-3856](https://youtrack.jetbrains.com/issue/KT-3856) Fix wrong "inner class inaccessible" diagnostic for extension to outer class
- [KT-3896](https://youtrack.jetbrains.com/issue/KT-3896), [KT-3883](https://youtrack.jetbrains.com/issue/KT-3883), [KT-4986](https://youtrack.jetbrains.com/issue/KT-4986) `do...while (true)` is now considered an infinite loop
- [KT-10445](https://youtrack.jetbrains.com/issue/KT-10445) Prohibit initialization of captured `val` in lambda or in local function
- [KT-10042](https://youtrack.jetbrains.com/issue/KT-10042) Correctly handle local classes and anonymous objects in control flow analysis
- [KT-11043](https://youtrack.jetbrains.com/issue/KT-11043) Prohibit complex expressions with class literals in annotation arguments
- [KT-10992](https://youtrack.jetbrains.com/issue/KT-10992), [KT-11007](https://youtrack.jetbrains.com/issue/KT-11007) Fix multiple problems related to smart casts
- [KT-11490](https://youtrack.jetbrains.com/issue/KT-11490) Prohibit nested intersection types in return position
- [KT-11411](https://youtrack.jetbrains.com/issue/KT-11411) Report "illegal noinline/crossinline" on parameter of subtype of function type
- [KT-3083](https://youtrack.jetbrains.com/issue/KT-3083) Report "conflicting overloads" for functions with parameter of type parameter type
- [KT-7265](https://youtrack.jetbrains.com/issue/KT-7265) Parse anonymous functions in blocks as expressions
- [KT-8246](https://youtrack.jetbrains.com/issue/KT-8246) Handle break/continue for outer loop correctly in case of try/finally in between
- [KT-11300](https://youtrack.jetbrains.com/issue/KT-11300) Report error on increment or augmented assignment when `get` is an operator but `set` is not
- Report warning about unused anonymous functions
- Improve callable reference type in some ambiguous cases
- Improve multiple diagnostic messages: [KT-10761](https://youtrack.jetbrains.com/issue/KT-10761), [KT-9760](https://youtrack.jetbrains.com/issue/KT-9760), [KT-10949](https://youtrack.jetbrains.com/issue/KT-10949), [KT-9887](https://youtrack.jetbrains.com/issue/KT-9887), [KT-9550](https://youtrack.jetbrains.com/issue/KT-9550), [KT-11239](https://youtrack.jetbrains.com/issue/KT-11239), [KT-11819](https://youtrack.jetbrains.com/issue/KT-11819)
- Fix several compiler bugs leading to exceptions: [KT-9820](https://youtrack.jetbrains.com/issue/KT-9820), [KT-11597](https://youtrack.jetbrains.com/issue/KT-11597), [KT-10983](https://youtrack.jetbrains.com/issue/KT-10983), [KT-10972](https://youtrack.jetbrains.com/issue/KT-10972), [KT-11287](https://youtrack.jetbrains.com/issue/KT-11287), [KT-11492](https://youtrack.jetbrains.com/issue/KT-11492), [KT-11765](https://youtrack.jetbrains.com/issue/KT-11765), [KT-11869](https://youtrack.jetbrains.com/issue/KT-11869)

#### JVM code generation

- [KT-8269](https://youtrack.jetbrains.com/issue/KT-8269), [KT-9246](https://youtrack.jetbrains.com/issue/KT-9246), [KT-10143](https://youtrack.jetbrains.com/issue/KT-10143) Fix visibility of protected classes in bytecode
- [KT-11363](https://youtrack.jetbrains.com/issue/KT-11363) Fix potential binary compatibility breakage on using `when` over enums in inline functions
- [KT-11762](https://youtrack.jetbrains.com/issue/KT-11762) Fix VerifyError caused by explicit loop variable type
- [KT-11645](https://youtrack.jetbrains.com/issue/KT-11645) Fix NoSuchFieldError on private const property in multi-file class
- [KT-9670](https://youtrack.jetbrains.com/issue/KT-9670) Optimize Class <-> KClass wrapping/unwrapping when getting values from annotation
- [KT-6842](https://youtrack.jetbrains.com/issue/KT-6842) Optimize unnecessary boxing and interface calls on iterating over ranges
- [KT-11025](https://youtrack.jetbrains.com/issue/KT-11025) Don't inline const val properties in non-annotation contexts
- [KT-5429](https://youtrack.jetbrains.com/issue/KT-5429) Write nullability annotations on extension receiver parameters
- [KT-11347](https://youtrack.jetbrains.com/issue/KT-11347) Preserve source file and line number of call site when inlining certain standard library functions
- [KT-11677](https://youtrack.jetbrains.com/issue/KT-11677) Write correct generic signatures for local classes in inlined lambdas
- [KT-12127](https://youtrack.jetbrains.com/issue/KT-12127) Do not write unnecessary generic signature for property delegate backing field
- Fix multiple issues leading to exceptions or bad bytecode being generated: [KT-11034](https://youtrack.jetbrains.com/issue/KT-11034), [KT-11519](https://youtrack.jetbrains.com/issue/KT-11519), [KT-11117](https://youtrack.jetbrains.com/issue/KT-11117), [KT-11479](https://youtrack.jetbrains.com/issue/KT-11479)

#### Java interoperability

- [KT-3068](https://youtrack.jetbrains.com/issue/KT-3068) Load contravariantly projected collections in Java (`List<? super T>`) as mutable collections in Kotlin (`MutableList<in T>`)
- [KT-11322](https://youtrack.jetbrains.com/issue/KT-11322) Do not lose type nullability information in SAM constructors
- [KT-11721](https://youtrack.jetbrains.com/issue/KT-11721) Fix wrong "Typechecker has run into recursive problem" error on calling Kotlin get function as synthetic Java property
- [KT-10691](https://youtrack.jetbrains.com/issue/KT-10691) Fix wrong "Inherited platform declarations clash" error on inheritance from generic Java class with overloaded methods

#### Command line compiler

- [KT-9546](https://youtrack.jetbrains.com/issue/KT-9546) Flush stdout and stderr before shutdown when executing scripts
- [KT-10605](https://youtrack.jetbrains.com/issue/KT-10605) Disable colored output on certain platforms to prevent crashes
- Report warning instead of error on unknown "-X" flags
- Remove the compiler option "Xmultifile-facades-open"

#### Compiler daemon

- Reduce read disk activity
- Fix compiler daemon JAR cache clearing on IDEA Ultimate

### Standard library

- [KT-11410](https://youtrack.jetbrains.com/issue/KT-11410) Reduce method count of the standard library by ~2k
- [KT-9990](https://youtrack.jetbrains.com/issue/KT-9990) Optimize snapshot operations to return special collection implementations when result is empty or has single element
- [KT-10794](https://youtrack.jetbrains.com/issue/KT-10794) EmptyList now implements RandomAccess
- [KT-10821](https://youtrack.jetbrains.com/issue/KT-10821) Create at most one wrapper sequence for adjacent drop/take operations on sequences
- [KT-11301](https://youtrack.jetbrains.com/issue/KT-11301) Make Map.plus accept Map out-projected by key type as either operand (receiver or parameter)
- [KT-11485](https://youtrack.jetbrains.com/issue/KT-11485) Remove implementations of some internal intrinsic functions
- [KT-11648](https://youtrack.jetbrains.com/issue/KT-11648) Add deprecated extension MutableList.remove to redirect to valid function removeAt
- [KT-11348](https://youtrack.jetbrains.com/issue/KT-11348) kotlin.test: Make inline methods `todo` and `currentStackTrace` `@InlineOnly` not to lose stack trace
- [KT-11745](https://youtrack.jetbrains.com/issue/KT-11745) Rename parameters of `String.subSequence` to match those of `CharSequence.subSequence`
- [KT-10953](https://youtrack.jetbrains.com/issue/KT-10953) Clarify parameter order of lambda function parameter of `*Indexed` functions
- [KT-10198](https://youtrack.jetbrains.com/issue/KT-10198) Improve docs for `binarySearch` functions
- [KT-9786](https://youtrack.jetbrains.com/issue/KT-9786) Improve docs for `trimIndent`/`trimMargin`

### Reflection

- [KT-9952](https://youtrack.jetbrains.com/issue/KT-9952) Improve `toString()` for lambdas and function expressions when kotlin-reflect.jar is available
- [KT-11433](https://youtrack.jetbrains.com/issue/KT-11433) Fix multiple resource leaks by closing InputStream instances
- [KT-8131](https://youtrack.jetbrains.com/issue/KT-8131) Fix exception from calling `KProperty.javaField` on a subclass
- [KT-10690](https://youtrack.jetbrains.com/issue/KT-10690) Support `javaMethod` and `kotlinFunction` for top level functions in a different file
- [KT-11447](https://youtrack.jetbrains.com/issue/KT-11447) Support reflection calls to multifile class members
- [KT-10892](https://youtrack.jetbrains.com/issue/KT-10892) Load annotations of const properties from multifile classes
- [KT-11258](https://youtrack.jetbrains.com/issue/KT-11258) Don't crash on requesting members of Java collection classes
- [KT-11502](https://youtrack.jetbrains.com/issue/KT-11502) Clarify KClass equality

### JS

- [KT-4124](https://youtrack.jetbrains.com/issue/KT-4124) Support nested classes
- [KT-11030](https://youtrack.jetbrains.com/issue/KT-11030) Support local classes
- [KT-7819](https://youtrack.jetbrains.com/issue/KT-7819) Support non-local returns in local lambdas
- [KT-6912](https://youtrack.jetbrains.com/issue/KT-6912) Safe calls (`x?.let { it }`) are now inlined
- [KT-2670](https://youtrack.jetbrains.com/issue/KT-2670) Support unsafe casts (`as`)
- [KT-7016](https://youtrack.jetbrains.com/issue/KT-7016), [KT-8012](https://youtrack.jetbrains.com/issue/KT-8012) Fix `is`-checks for reified type parameters
- [KT-7038](https://youtrack.jetbrains.com/issue/KT-7038) Avoid unwanted side effects on `is`-checks for nullable types
- [KT-10614](https://youtrack.jetbrains.com/issue/KT-10614) Copy array on vararg call with spread operator
- [KT-10785](https://youtrack.jetbrains.com/issue/KT-10785) Correctly translate property names and receiver instances in assignment operations
- [KT-11611](https://youtrack.jetbrains.com/issue/KT-11611) Fix translation of default value of secondary constructor's functional parameter
- [KT-11100](https://youtrack.jetbrains.com/issue/KT-11100) Fix generation of `invoke` on objects and companion objects
- [KT-11823](https://youtrack.jetbrains.com/issue/KT-11823) Fix capturing of outer class' `this` in inner's lambdas
- [KT-11996](https://youtrack.jetbrains.com/issue/KT-11996) Fix translation of a call to a private member of an outer class from an inner class which is a subtype of the outer class
- [KT-10667](https://youtrack.jetbrains.com/issue/KT-10667) Support inheritance from nested built-in types such as Map.Entry
- [KT-7480](https://youtrack.jetbrains.com/issue/KT-7480) Remove declarations of LinkedList, SortedSet, TreeSet, Enumeration
- [KT-3064](https://youtrack.jetbrains.com/issue/KT-3064) Implement `CharSequence.repeat`

### IDE

New features:

- Spring Support
  - [KT-11098](https://youtrack.jetbrains.com/issue/KT-11098) Inspection on final classes/functions annotated with Spring `@Configuration`/`@Component`/`@Bean`
  - [KT-11405](https://youtrack.jetbrains.com/issue/KT-11405) Navigation and Find Usages for Spring beans referenced in annotation arguments and BeanFactory method calls
  - [KT-3741](https://youtrack.jetbrains.com/issue/KT-3741) Show Spring-specific line markers on Kotlin classes
  - [KT-11406](https://youtrack.jetbrains.com/issue/KT-11406) Support Spring EL injections inside of Kotlin string literals
  - [KT-11604](https://youtrack.jetbrains.com/issue/KT-11604) Support "Configure Spring facet" inspection on Kotlin classes
  - [KT-11407](https://youtrack.jetbrains.com/issue/KT-11407) Implement "Generate Spring Dependency..." actions
  - [KT-11408](https://youtrack.jetbrains.com/issue/KT-11408) Implement "Generate `@Autowired` Dependency..." action
  - [KT-11652](https://youtrack.jetbrains.com/issue/KT-11652) Rename bean attributes mentioned in Spring XML config together with corresponding Kotlin declarations
- Enable precise incremental compilation by default in non-Maven/Gradle projects
- [KT-11612](https://youtrack.jetbrains.com/issue/KT-11612) Highlight named arguments
- [KT-7715](https://youtrack.jetbrains.com/issue/KT-7715) Highlight `var`s that can be replaced by `val`s
- [KT-5208](https://youtrack.jetbrains.com/issue/KT-5208) Intention action to convert string to raw string and back
- [KT-11078](https://youtrack.jetbrains.com/issue/KT-11078) Quick fix to remove `.java` when KClass is expected
- [KT-1494](https://youtrack.jetbrains.com/issue/KT-1494) Inspection to highlight public members with no documentation
- [KT-8473](https://youtrack.jetbrains.com/issue/KT-8473) Intention action to implement interface or abstract class
- [KT-10299](https://youtrack.jetbrains.com/issue/KT-10299) Inspection to warn on array properties in data classes
- [KT-6674](https://youtrack.jetbrains.com/issue/KT-6674) Inspection to warn on protected symbols in effectively final classes
- [KT-11576](https://youtrack.jetbrains.com/issue/KT-11576) Quick fix to suppress "Unused symbol" warning based on annotations on the declaration
- [KT-10063](https://youtrack.jetbrains.com/issue/KT-10063) Quick fix for adding `arrayOf` wrapper for annotation parameters
- [KT-10476](https://youtrack.jetbrains.com/issue/KT-10476) Quick fix for converting primitive types
- [KT-10859](https://youtrack.jetbrains.com/issue/KT-10859) Quick fix to make `var` with private setter final
- [KT-9498](https://youtrack.jetbrains.com/issue/KT-9498) Quick fix to specify property type
- [KT-10509](https://youtrack.jetbrains.com/issue/KT-10509) Quick fix to simplify condition with senseless comparison
- [KT-11404](https://youtrack.jetbrains.com/issue/KT-11404) Quick fix to let type implement missing interface
- [KT-6785](https://youtrack.jetbrains.com/issue/KT-6785), [KT-10013](https://youtrack.jetbrains.com/issue/KT-10013), [KT-9996](https://youtrack.jetbrains.com/issue/KT-9996), [KT-11675](https://youtrack.jetbrains.com/issue/KT-11675) Support Smart Enter for trailing lambda argument, try/catch/finally, property setter, init block
- Add `kotlinClassName()` and `kotlinFunctionName()` macros for use in live templates
- Auto-configure EAP-repository during Kotlin Maven and Gradle project set up

Issues fixed:

- [KT-11678](https://youtrack.jetbrains.com/issue/KT-11678), [KT-4768](https://youtrack.jetbrains.com/issue/KT-4768) Support navigation to Kotlin libraries from Java sources
- [KT-9401](https://youtrack.jetbrains.com/issue/KT-9401) Support Change Signature quick fix for Java -> Kotlin case
- [KT-8592](https://youtrack.jetbrains.com/issue/KT-8592) Fix "Choose sources" for Kotlin files
- [KT-11256](https://youtrack.jetbrains.com/issue/KT-11256) Fix Navigate to declaration for Java constructor with `@NotNull` parameter
- [KT-11018](https://youtrack.jetbrains.com/issue/KT-11018) Fix `var`s shown in Ctrl + Mouse Hover as `val`s
- [KT-5105](https://youtrack.jetbrains.com/issue/KT-5105), [KT-11024](https://youtrack.jetbrains.com/issue/KT-11024) Improve incompatible ABI versions editor strap, show the hint on how to resolve the problem
- [KT-11638](https://youtrack.jetbrains.com/issue/KT-11638) Fixed `hashCode()` implementation in "Generate equals/hashCode" action
- [KT-10971](https://youtrack.jetbrains.com/issue/KT-10971) Pull Members Up: Always insert spaces between keywords
- [KT-11476](https://youtrack.jetbrains.com/issue/KT-11476), [KT-4175](https://youtrack.jetbrains.com/issue/KT-4175), [KT-10965](https://youtrack.jetbrains.com/issue/KT-10965), [KT-11076](https://youtrack.jetbrains.com/issue/KT-11076) Formatter: fix multiple issues regarding space handling
- [KT-9025](https://youtrack.jetbrains.com/issue/KT-9025) Improve "Create Kotlin Java runtime library" dialog usability
- [KT-11481](https://youtrack.jetbrains.com/issue/KT-11481) Fix "Add import" intention not being available for `is` branches in when
- [KT-10619](https://youtrack.jetbrains.com/issue/KT-10619) Fix completion after package name in annotation
- [KT-10621](https://youtrack.jetbrains.com/issue/KT-10621) Do not show non-top level packages after `@` in completion
- [KT-11295](https://youtrack.jetbrains.com/issue/KT-11295) "Convert string to template" intention: fix exception on certain code
- [KT-10750](https://youtrack.jetbrains.com/issue/KT-10750), [KT-11424](https://youtrack.jetbrains.com/issue/KT-11424) "Convert if to when" intention now detects effectively else branches in subsequent code and performs more accurate comment handling
- Configure Kotlin: show only changed files in the notification "Kotlin not configured", restore all changed files in undo action
- [KT-11556](https://youtrack.jetbrains.com/issue/KT-11556) Do not show "Kotlin not configured" for Kotlin JS projects
- [KT-11593](https://youtrack.jetbrains.com/issue/KT-11593) Fix "Configure Kotlin" action for Gradle projects in IDEA 2016
- [KT-11077](https://youtrack.jetbrains.com/issue/KT-11077) Use new built-in definition file format (`.kotlin_builtins` files)
- [KT-5728](https://youtrack.jetbrains.com/issue/KT-5728) Remove closing curly brace in a string template when opening one is deleted
- [KT-10883](https://youtrack.jetbrains.com/issue/KT-10883) "Explicit get or set call" quick fix: do not move caret too far away
- [KT-5717](https://youtrack.jetbrains.com/issue/KT-5717) "Replace `when` with `if`": do not lose comments
- [KT-10797](https://youtrack.jetbrains.com/issue/KT-10797) "Replace with operator" intention is not available anymore for non-`operator` functions
- [KT-11529](https://youtrack.jetbrains.com/issue/KT-11529) Highlighting range for unresolved annotation name does not include `@` now
- [KT-11178](https://youtrack.jetbrains.com/issue/KT-11178) Don't show "Change type arguments" fix when there's nothing to change
- [KT-11789](https://youtrack.jetbrains.com/issue/KT-11789) Don't interpret annotations inside Markdown code blocks as KDoc tags
- [KT-11702](https://youtrack.jetbrains.com/issue/KT-11702) Fixed resolution of Kotlin beans with custom name
- [KT-11689](https://youtrack.jetbrains.com/issue/KT-11689) Fixed exception on attempt to navigate to Kotlin file from Spring notification balloon
- [KT-11725](https://youtrack.jetbrains.com/issue/KT-11725) Fixed renaming of injected SpEL references
- [KT-11720](https://youtrack.jetbrains.com/issue/KT-11720) Fixed renaming of Kotlin beans through SpEL references
- [KT-11719](https://youtrack.jetbrains.com/issue/KT-11719) Fixed renaming of Kotlin parameters references in XML files
- [KT-11736](https://youtrack.jetbrains.com/issue/KT-11736) Fixed searching of Java usages for @JvmStatic properties and @JvmStatic @JvmOverloads functions
- [KT-11862](https://youtrack.jetbrains.com/issue/KT-11862) Fixed bogus warnings about unresolved types in the Change Signature dialog
- Fix several issues leading to exceptions: [KT-11579](https://youtrack.jetbrains.com/issue/KT-11579), [KT-11580](https://youtrack.jetbrains.com/issue/KT-11580), [KT-11777](https://youtrack.jetbrains.com/issue/KT-11777), [KT-11868](https://youtrack.jetbrains.com/issue/KT-11868), [KT-11845](https://youtrack.jetbrains.com/issue/KT-11845), [KT-11486](https://youtrack.jetbrains.com/issue/KT-11486)
- Fixed NoSuchFieldException in Kotlin module settings on IDEA Ultimate

#### Debugger

- [KT-11705](https://youtrack.jetbrains.com/issue/KT-11705) "Smart step into" no longer skips methods from subclasses
- Debugger can now distinguish nested inline arguments
- [KT-11326](https://youtrack.jetbrains.com/issue/KT-11326) Support private classes in Evaluate Expression
- [KT-11455](https://youtrack.jetbrains.com/issue/KT-11455) Fix Evaluate Expression behavior for files with errors in sources
- [KT-10670](https://youtrack.jetbrains.com/issue/KT-10670) Fix Evaluate Expression behavior for inline functions with default parameters
- [KT-11380](https://youtrack.jetbrains.com/issue/KT-11380) Evaluate Expression now handles smart casts correctly
- [KT-10148](https://youtrack.jetbrains.com/issue/KT-10148) Do not suggest methods from outer context in "Smart step into"
- Fix Evaluate Expression for expression created for array element
- Complete private members from libraries in Evaluate Expression
- [KT-11578](https://youtrack.jetbrains.com/issue/KT-11578) Evaluate Expression: do not highlight completion variants from nullable receiver with grey
- [KT-6805](https://youtrack.jetbrains.com/issue/KT-6805) Convert Java expression to Kotlin when opening Evaluate Expression from Variables view
- [KT-11927](https://youtrack.jetbrains.com/issue/KT-11927) Fix "ambiguous import" error when invoking Evaluate Expression from Variables view for some field
- [KT-11831](https://youtrack.jetbrains.com/issue/KT-11831) Fix Evaluate Expression for values of raw types
- Show error message when debug info for some local variable is corrupted
- Avoid 1s delay in completion in debugger fields if session is not stopped on a breakpoint
- Avoid cast to runtime type unavailable in current scope
- Fix text with line breaks in popup with line breakpoint variants
- Fix breakpoints inside inline functions in libraries sources
- Allow breakpoints at catch clause declaration
- [KT-11848](https://youtrack.jetbrains.com/issue/KT-11848) Fix breakpoints inside generic crossinline lambda argument body
- [KT-11932](https://youtrack.jetbrains.com/issue/KT-11932) Fix Step Over for `while` loop condition

### Java to Kotlin converter

- Protected members used outside of inheritors are converted as public
- Support conversion for annotation constructor calls
- Place comments from the middle of the call to the end
- Drop line breaks between operator arguments (except `+`, `-`, `&&` and `||`)
- Add non-null assertions on call site for non-null parameters
- Specify type for variables with anonymous type if they have write accesses
- [KT-11587](https://youtrack.jetbrains.com/issue/KT-11587) Fix conversion of static field accesses from other Java class
- [KT-6800](https://youtrack.jetbrains.com/issue/KT-6800) Quote `$` symbols in converted strings
- [KT-11126](https://youtrack.jetbrains.com/issue/KT-11126) Convert annotations in annotations parameters correctly
- [KT-11600](https://youtrack.jetbrains.com/issue/KT-11600) Do not produce unresolved `toArray` calls for Java `Collection#toArray(T[])`
- [KT-11544](https://youtrack.jetbrains.com/issue/KT-11544) Fix conversion of uninitialized non-final field
- [KT-10604](https://youtrack.jetbrains.com/issue/KT-10604) Fix conversion of scratch files
- [KT-11543](https://youtrack.jetbrains.com/issue/KT-11543) Do not produce unnecessary casts of non-nullable expression to nullable type
- [KT-11160](https://youtrack.jetbrains.com/issue/KT-11160) Fix IDE freeze

### Android

- [KT-7729](https://youtrack.jetbrains.com/issue/KT-7729) Add Android Lint checks for Kotlin (from Android Studio 1.5)
- [KT-11487](https://youtrack.jetbrains.com/issue/KT-11487) Fixed sequential build with kapt and stubs enabled when Kotlin source file was modified and no Java source files were modified
- [KT-11264](https://youtrack.jetbrains.com/issue/KT-11264) Action to create new activity in Kotlin
- [KT-11201](https://youtrack.jetbrains.com/issue/KT-11201) Do not ignore items with similar names in kapt
- [KT-11944](https://youtrack.jetbrains.com/issue/KT-11944) Rename Android Extensions imports when the layout file is renamed/deleted/added
- [KT-10321](https://youtrack.jetbrains.com/issue/KT-10321) Do not upcast ViewStub to View
- [KT-10841](https://youtrack.jetbrains.com/issue/KT-10841) Support `@android:id/*` IDs in Android Extensions

### Maven

- [KT-2917](https://youtrack.jetbrains.com/issue/KT-2917), [KT-11261](https://youtrack.jetbrains.com/issue/KT-11261) Maven archetype for new Kotlin projects

### Gradle

- [KT-8487](https://youtrack.jetbrains.com/issue/KT-8487) Experimental support for incremental compilation with project property `kotlin.incremental`
- [KT-11350](https://youtrack.jetbrains.com/issue/KT-11350) Fixed a bug causing Java rebuild when both Java and Kotlin are up-to-date
- [KT-10507](https://youtrack.jetbrains.com/issue/KT-10507) Fix IllegalArgumentException "Missing extension point" on parallel builds
- [KT-10932](https://youtrack.jetbrains.com/issue/KT-10932) Prevent compile tasks from running when nothing changes
- [KT-11993](https://youtrack.jetbrains.com/issue/KT-11993) Fix NoSuchMethodError on access to internal members in production from tests (IDEA 2016+)

## 1.0.1-2

### Compiler

- [KT-11584](https://youtrack.jetbrains.com/issue/KT-11584), [KT-11514](https://youtrack.jetbrains.com/issue/KT-11514) Correct comparison of Long! / Double! with integer constant
- [KT-11590](https://youtrack.jetbrains.com/issue/KT-11590) SAM adapter for inline function corrected

## 1.0.1-1

### Compiler

- [KT-11468](https://youtrack.jetbrains.com/issue/KT-11468) More correct use-site / declaration-site variance combination handling
- [KT-11478](https://youtrack.jetbrains.com/issue/KT-11478) "Couldn't inline method call" internal compiler error fixed

## 1.0.1

### Compiler

Analysis & diagnostics issues fixed:

- [KT-2277](https://youtrack.jetbrains.com/issue/KT-2277) Local function declarations are now checked for overload conflicts
- [KT-3602](https://youtrack.jetbrains.com/issue/KT-3602)  Special diagnostic is reported now on nullable ‘for’ range
- [KT-10775](https://youtrack.jetbrains.com/issue/KT-10775) No compilation exception for empty when
- [KT-10952](https://youtrack.jetbrains.com/issue/KT-10952) False deprecation warnings removed
- [KT-10934](https://youtrack.jetbrains.com/issue/KT-10934) Type inference improved for whens
- [KT-10902](https://youtrack.jetbrains.com/issue/KT-10902) Redeclaration is reported for top-level property vs classifier conflict
- [KT-9985](https://youtrack.jetbrains.com/issue/KT-9985)  Correct handling of safe call arguments in generic functions
- [KT-10856](https://youtrack.jetbrains.com/issue/KT-10856) Diagnostic about projected out member is reported correctly on calls with smart cast receiver
- [KT-5190](https://youtrack.jetbrains.com/issue/KT-5190)  Calls of Java 8 Stream.collect
- [KT-11109](https://youtrack.jetbrains.com/issue/KT-11109) Warning is reported on Strictfp annotation on a class because it's not supported yet
- [KT-10686](https://youtrack.jetbrains.com/issue/KT-10686) Support generic constructors defined in Java
- [KT-6958](https://youtrack.jetbrains.com/issue/KT-6958)  Fixed resolution for overloaded functions with extension lambdas
- [KT-10765](https://youtrack.jetbrains.com/issue/KT-10765) Correct handling of overload conflict between constructor and function in JPS
- [KT-10752](https://youtrack.jetbrains.com/issue/KT-10752) If inferred type for an expression refers to a non-accessible Java class, it's a compiler error to prevent IAE in runtime
- [KT-7415](https://youtrack.jetbrains.com/issue/KT-7415) Approximation of captured types in signatures
- [KT-10913](https://youtrack.jetbrains.com/issue/KT-10913), [KT-10186](https://youtrack.jetbrains.com/issue/KT-10186), [KT-5198](https://youtrack.jetbrains.com/issue/KT-5198) False “unreachable code” fixed for various situations
- Minor: [KT-3680](https://youtrack.jetbrains.com/issue/KT-3680), [KT-9702](https://youtrack.jetbrains.com/issue/KT-9702), [KT-8776](https://youtrack.jetbrains.com/issue/KT-8776), [KT-6745](https://youtrack.jetbrains.com/issue/KT-6745), [KT-10919](https://youtrack.jetbrains.com/issue/KT-10919), [KT-9548](https://youtrack.jetbrains.com/issue/KT-9548)

JVM code generation issues fixed:

- [KT-11153](https://youtrack.jetbrains.com/issue/KT-11153) NoClassDefFoundError is fixed on primitive iterators during boxing optimization
- [KT-7319](https://youtrack.jetbrains.com/issue/KT-7319)  Correct parameter names for @JvmOverloads-generated methods
- [KT-10425](https://youtrack.jetbrains.com/issue/KT-10425) Non-const values of member properties are not inlined now
- [KT-11163](https://youtrack.jetbrains.com/issue/KT-11163) Correct calls of custom compareTo on primitives
- [KT-11081](https://youtrack.jetbrains.com/issue/KT-11081) Reified type parameters are correctly stored in anonymous objects
- [KT-11121](https://youtrack.jetbrains.com/issue/KT-11121) Generic properties generation is fixed for interfaces
- [KT-11285](https://youtrack.jetbrains.com/issue/KT-11285), [KT-10958](https://youtrack.jetbrains.com/issue/KT-10958) Special bridge generation refined
- [KT-10313](https://youtrack.jetbrains.com/issue/KT-10313), [KT-11190](https://youtrack.jetbrains.com/issue/KT-11190), [KT-11192](https://youtrack.jetbrains.com/issue/KT-11192), [KT-11130](https://youtrack.jetbrains.com/issue/KT-11130) Diagnostics and bytecode fixed for various operations with Long
- [KT-11203](https://youtrack.jetbrains.com/issue/KT-11203), [KT-11191](https://youtrack.jetbrains.com/issue/KT-11191), [KT-11206](https://youtrack.jetbrains.com/issue/KT-11206), [KT-8505](https://youtrack.jetbrains.com/issue/KT-8505), [KT-11203](https://youtrack.jetbrains.com/issue/KT-11203) Handling of increment / decrement for collection elements with user-defined get / set fixed
- [KT-9739](https://youtrack.jetbrains.com/issue/KT-9739)  Backticked names with spaces are generated correctly

JS translator issues fixed:

- [KT-7683](https://youtrack.jetbrains.com/issue/KT-7683), [KT-11027](https://youtrack.jetbrains.com/issue/KT-11027) correct handling of in / !in inside when expressions

### Standard library

- [KT-10579](https://youtrack.jetbrains.com/issue/KT-10579) Improved performance of sum() and average() for arrays
- [KT-10821](https://youtrack.jetbrains.com/issue/KT-10821) Improved performance of drop() / take() for sequences

### Reflection

- [KT-10840](https://youtrack.jetbrains.com/issue/KT-10840) Fix annotations on Java elements in reflection

### IDE

New features:

- Compatibility with IDEA 2016
- Kotlin Education Plugin (for IDEA 2016)
- [KT-9752](https://youtrack.jetbrains.com/issue/KT-9752)  More usable file chooser for "Move declaration to another file"
- [KT-9697](https://youtrack.jetbrains.com/issue/KT-9697)  Move method to companion object and back
- [KT-7443](https://youtrack.jetbrains.com/issue/KT-7443) Inspection + intention to replace assert (x != null) with "!!" or elvis

General issues fixed:

- [KT-11277](https://youtrack.jetbrains.com/issue/KT-11277) Correct moving of Java classes from project view
- [KT-11256](https://youtrack.jetbrains.com/issue/KT-11256) Navigate Declaration fixed for Java classes with @NotNull parameter in constructor
- [KT-10553](https://youtrack.jetbrains.com/issue/KT-10553) A warning provided when Refactor / Move result is not compilable due to visibility problems
- [KT-11039](https://youtrack.jetbrains.com/issue/KT-11039) Parameter names are now not missing in parameter info and completion for compiled java code used from kotlin
- [KT-10204](https://youtrack.jetbrains.com/issue/KT-10204) Highlight usages in file is working now for function parameter
- [KT-10954](https://youtrack.jetbrains.com/issue/KT-10954) Introduce Parameter (Ctrl+Alt+P) fixed when default value is a simple name reference
- [KT-10776](https://youtrack.jetbrains.com/issue/KT-10776) Intentions: "Convert to lambda expression" works now for empty function body
- [KT-10815](https://youtrack.jetbrains.com/issue/KT-10815) Generate equals() and hashCode() is no more suggested for interfaces
- [KT-10818](https://youtrack.jetbrains.com/issue/KT-10818) "Initialize with constructor parameter" fixed
- [KT-8876](https://youtrack.jetbrains.com/issue/KT-8876) "Convert member to extension" now removes modality modifiers (open / final)
- [KT-10800](https://youtrack.jetbrains.com/issue/KT-10800) Create enum entry now adds comma after a new entry
- [KT-10552](https://youtrack.jetbrains.com/issue/KT-10552) Pull Members Up now takes visibility conflicts into account
- [KT-10978](https://youtrack.jetbrains.com/issue/KT-10978) Partially fixed, completion for JOOQ became ~ 10 times faster
- [KT-10940](https://youtrack.jetbrains.com/issue/KT-10940) Reference search optimized for convention functions
- [KT-9026](https://youtrack.jetbrains.com/issue/KT-9026)  Editor no more locks up during scala file viewing
- [KT-11142](https://youtrack.jetbrains.com/issue/KT-11142), [KT-11276](https://youtrack.jetbrains.com/issue/KT-11276) Darkula scheme appearance corrected for Kotlin
- Minor: [KT-10778](https://youtrack.jetbrains.com/issue/KT-10778), [KT-10763](https://youtrack.jetbrains.com/issue/KT-10763), [KT-10908](https://youtrack.jetbrains.com/issue/KT-10908), [KT-10345](https://youtrack.jetbrains.com/issue/KT-10345), [KT-10696](https://youtrack.jetbrains.com/issue/KT-10696), [KT-11041](https://youtrack.jetbrains.com/issue/KT-11041), [KT-9434](https://youtrack.jetbrains.com/issue/KT-9434), [KT-8744](https://youtrack.jetbrains.com/issue/KT-8744), [KT-9738](https://youtrack.jetbrains.com/issue/KT-9738), [KT-10912](https://youtrack.jetbrains.com/issue/KT-10912)

Configuration issues fixed:

- [KT-11213](https://youtrack.jetbrains.com/issue/KT-11213) Kotlin plugin version corrected in build.gradle
- [KT-10918](https://youtrack.jetbrains.com/issue/KT-10918) "Update Kotlin runtime" action does not try to update the runtime coming in from Gradle
- [KT-11072](https://youtrack.jetbrains.com/issue/KT-11072) Libraries in maven, gradle and ide systems are never more detected as runtime libraries
- [KT-10489](https://youtrack.jetbrains.com/issue/KT-10489) Configuration messages are aggregated into one notification
- [KT-10831](https://youtrack.jetbrains.com/issue/KT-10831) Configure Kotlin in Project: "All modules containing Kotlin files" does not list modules not containing Kotlin files
- [KT-10366](https://youtrack.jetbrains.com/issue/KT-10366) Gradle import: no fake "Configure Kotlin" notification on project creating

Debugger issues fixed:

- [KT-10827](https://youtrack.jetbrains.com/issue/KT-10827) Fixed debugger stepping for inline calls
- [KT-10780](https://youtrack.jetbrains.com/issue/KT-10780) Breakpoints in a lazy property work correctly
- [KT-10634](https://youtrack.jetbrains.com/issue/KT-10634) Watches can now use private overloaded functions
- [KT-10611](https://youtrack.jetbrains.com/issue/KT-10611) Line breakpoints now can be created inside lambda in init block
- [KT-10673](https://youtrack.jetbrains.com/issue/KT-10673) Breakpoints inside lambda are no more ignored in presence of crossinline function parameter
- [KT-11318](https://youtrack.jetbrains.com/issue/KT-11318) Stepping inside for each is optimized
- [KT-3873](https://youtrack.jetbrains.com/issue/KT-3873)  Editing code while standing on breakpoint is optimized
- [KT-7261](https://youtrack.jetbrains.com/issue/KT-7261), [KT-7266](https://youtrack.jetbrains.com/issue/KT-7266), [KT-10672](https://youtrack.jetbrains.com/issue/KT-10672) Evaluate expression applicability corrected

### Tools

- [KT-7943](https://youtrack.jetbrains.com/issue/KT-7943), [KT-10127](https://youtrack.jetbrains.com/issue/KT-10127) Overhead removed in Kotlin Gradle Plugin
- [KT-11351](https://youtrack.jetbrains.com/issue/KT-11351) Fixed NoSuchMethodError with Gradle 2.12
