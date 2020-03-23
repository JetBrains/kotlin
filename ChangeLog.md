# CHANGELOG

## 1.4-M1

### Compiler

#### New Features

- [`KT-4240`](https://youtrack.jetbrains.com/issue/KT-4240) Type inference possible improvements: analyze lambda with expected type from the outer call
- [`KT-7304`](https://youtrack.jetbrains.com/issue/KT-7304) Smart-casts and generic calls with multiple bounds on type parameters
- [`KT-7745`](https://youtrack.jetbrains.com/issue/KT-7745) Support named arguments in their own position even if the result appears as mixed
- [`KT-7770`](https://youtrack.jetbrains.com/issue/KT-7770) SAM for Kotlin classes
- [`KT-8834`](https://youtrack.jetbrains.com/issue/KT-8834) Support function references with default values as other function types
- [`KT-10930`](https://youtrack.jetbrains.com/issue/KT-10930) Expected type isn't taken into account for delegated properties
- [`KT-11723`](https://youtrack.jetbrains.com/issue/KT-11723) Support coercion to Unit in callable reference resolution
- [`KT-14416`](https://youtrack.jetbrains.com/issue/KT-14416) Support of @PolymorphicSignature in Kotlin compiler
- [`KT-16873`](https://youtrack.jetbrains.com/issue/KT-16873) Support COERSION_TO_UNIT for suspend lambdas
- [`KT-17643`](https://youtrack.jetbrains.com/issue/KT-17643) Inferring type of Pair based on known Map type
- [`KT-19869`](https://youtrack.jetbrains.com/issue/KT-19869) Support function references to functions with vararg if expected type ends with repeated vararg element type
- [`KT-21178`](https://youtrack.jetbrains.com/issue/KT-21178) Prohibit access of protected members inside public inline members
- [`KT-21368`](https://youtrack.jetbrains.com/issue/KT-21368) Improve type inference
- [`KT-25866`](https://youtrack.jetbrains.com/issue/KT-25866) Iterable.forEach does not accept functions that return non-Unit values
- [`KT-26165`](https://youtrack.jetbrains.com/issue/KT-26165) Support VarHandle in JVM codegen
- [`KT-27582`](https://youtrack.jetbrains.com/issue/KT-27582) Allow contracts on final non-override members
- [`KT-28298`](https://youtrack.jetbrains.com/issue/KT-28298) Allow references to generic (reified) type parameters in contracts
- [`KT-31230`](https://youtrack.jetbrains.com/issue/KT-31230) Refine rules for allowed Array-based class literals on different platforms: allow `Array::class` everywhere, disallow `Array<...>::class` on non-JVM
- [`KT-31244`](https://youtrack.jetbrains.com/issue/KT-31244) Choose Java field during overload resolution with a pure Kotlin property
- [`KT-31734`](https://youtrack.jetbrains.com/issue/KT-31734) Empty parameter list required on Annotations of function types
- [`KT-33990`](https://youtrack.jetbrains.com/issue/KT-33990) Type argument isn't checked during resolution part
- [`KT-33413`](https://youtrack.jetbrains.com/issue/KT-33413) Allow 'break' and 'continue' in 'when' statement to point to innermost surrounding loop
- [`KT-34743`](https://youtrack.jetbrains.com/issue/KT-34743) Support trailing comma in the compiler
- [`KT-34847`](https://youtrack.jetbrains.com/issue/KT-34847) Lift restrictions from `kotlin.Result`

#### Fixes

- [`KT-2869`](https://youtrack.jetbrains.com/issue/KT-2869) Incorrect resolve with 'unsafe call error' and generics
- [`KT-3630`](https://youtrack.jetbrains.com/issue/KT-3630) Extension property (generic function type) does not work
- [`KT-3668`](https://youtrack.jetbrains.com/issue/KT-3668) Infer type parameters for extension 'get' in delegated property
- [`KT-3850`](https://youtrack.jetbrains.com/issue/KT-3850) Receiver check fails when type parameter has another parameter as a bound
- [`KT-3884`](https://youtrack.jetbrains.com/issue/KT-3884) Generic candidate with contradiction is preferred over matching global function
- [`KT-4625`](https://youtrack.jetbrains.com/issue/KT-4625) Poor error highlighting when assigning not matched type to index operator
- [`KT-5449`](https://youtrack.jetbrains.com/issue/KT-5449) Wrong resolve when functions differ only in the nullability of generic type
- [`KT-5606`](https://youtrack.jetbrains.com/issue/KT-5606) "Type mismatch" in Java constructor call with SAM lambda and `vararg` parameter
- [`KT-6005`](https://youtrack.jetbrains.com/issue/KT-6005) Type inference problem in sam constructors
- [`KT-6591`](https://youtrack.jetbrains.com/issue/KT-6591) Overloaded generic extension function call with null argument resolved incorrectly
- [`KT-6812`](https://youtrack.jetbrains.com/issue/KT-6812) Type inference fails when passing a null instead of a generic type
- [`KT-7298`](https://youtrack.jetbrains.com/issue/KT-7298) Bogus type inference error in generic method call translated from Java
- [`KT-7301`](https://youtrack.jetbrains.com/issue/KT-7301) Type inference error in Kotlin code translated from Java
- [`KT-7333`](https://youtrack.jetbrains.com/issue/KT-7333) Type inference fails with star-projections in code translated from Java
- [`KT-7363`](https://youtrack.jetbrains.com/issue/KT-7363) Kotlin code with star-projections translated from Java does not typecheck
- [`KT-7378`](https://youtrack.jetbrains.com/issue/KT-7378) 3-dimension array type inference fail
- [`KT-7410`](https://youtrack.jetbrains.com/issue/KT-7410) Call resolution error appears only after adding non-applicable overload
- [`KT-7420`](https://youtrack.jetbrains.com/issue/KT-7420) Type inference sometimes infers less specific type than in Java
- [`KT-7758`](https://youtrack.jetbrains.com/issue/KT-7758) Type of lambda can't be infered 
- [`KT-8218`](https://youtrack.jetbrains.com/issue/KT-8218) Wrong 'equals' for generic types with platform type and error type
- [`KT-8265`](https://youtrack.jetbrains.com/issue/KT-8265) Non-typesafe program is compiled without errors
- [`KT-8637`](https://youtrack.jetbrains.com/issue/KT-8637) Useless diagnostics for type parameters with unsafe nullability 
- [`KT-8966`](https://youtrack.jetbrains.com/issue/KT-8966) Smart casts don't work with implicit receiver and extension on type parameter with bounds
- [`KT-10265`](https://youtrack.jetbrains.com/issue/KT-10265) Type inference problem when using sealed class and interfaces
- [`KT-10364`](https://youtrack.jetbrains.com/issue/KT-10364) Call completeCall on variable before invoke resolution
- [`KT-10612`](https://youtrack.jetbrains.com/issue/KT-10612) java.util.Comparator.comparing type inference
- [`KT-10628`](https://youtrack.jetbrains.com/issue/KT-10628) Wrong type mismatch with star projection of inner class inside use-site projected type 
- [`KT-10662`](https://youtrack.jetbrains.com/issue/KT-10662) Smartcast with not-null assertion
- [`KT-10681`](https://youtrack.jetbrains.com/issue/KT-10681) Explicit type arguments not taken into account when determining applicable overloads of a generic function
- [`KT-10755`](https://youtrack.jetbrains.com/issue/KT-10755) Not "least" common super-type is selected for nested 'if' result in presence of multiple inheritance 
- [`KT-10929`](https://youtrack.jetbrains.com/issue/KT-10929) Type inference based on receiver type doesn't work for delegated properties in some cases
- [`KT-10962`](https://youtrack.jetbrains.com/issue/KT-10962) Wrong resolution when argument has unstable DataFlowValue
- [`KT-11108`](https://youtrack.jetbrains.com/issue/KT-11108) RxJava failed platform type inference
- [`KT-11137`](https://youtrack.jetbrains.com/issue/KT-11137) Java synthetic property does not function for a type with projection
- [`KT-11144`](https://youtrack.jetbrains.com/issue/KT-11144) UninferredParameterTypeConstructor exception during build 
- [`KT-11184`](https://youtrack.jetbrains.com/issue/KT-11184) Type inference failed for combination of safe-call, elvis, HashSet and emptySet 
- [`KT-11218`](https://youtrack.jetbrains.com/issue/KT-11218) Type inference incorrectly infers nullable type for type parameter
- [`KT-11323`](https://youtrack.jetbrains.com/issue/KT-11323) Type inference failed in call with lambda returning emptyList
- [`KT-11331`](https://youtrack.jetbrains.com/issue/KT-11331) Unexpected "Type inference failed" in SAM-conversion to projected type
- [`KT-11444`](https://youtrack.jetbrains.com/issue/KT-11444) Type inference fails
- [`KT-11664`](https://youtrack.jetbrains.com/issue/KT-11664) Disfunctional inference with nullable type parameters
- [`KT-11894`](https://youtrack.jetbrains.com/issue/KT-11894) Type substitution bug related platform types
- [`KT-11897`](https://youtrack.jetbrains.com/issue/KT-11897) No error REIFIED_TYPE_FORBIDDEN_SUBSTITUTION on captured type
- [`KT-11898`](https://youtrack.jetbrains.com/issue/KT-11898) Type inference error related to captured types
- [`KT-12036`](https://youtrack.jetbrains.com/issue/KT-12036) Type inference failed
- [`KT-12038`](https://youtrack.jetbrains.com/issue/KT-12038) non-null checks and inference
- [`KT-12190`](https://youtrack.jetbrains.com/issue/KT-12190) Type inference for TreeMap type parameters from expected type doesn't work when passing comparator.
- [`KT-12684`](https://youtrack.jetbrains.com/issue/KT-12684) A problem with reified type-parameters and smart-casts
- [`KT-12833`](https://youtrack.jetbrains.com/issue/KT-12833) 'it' does not work in typed containers of lambdas
- [`KT-13002`](https://youtrack.jetbrains.com/issue/KT-13002) "Error type encountered: UninferredParameterTypeConstructor" with elvis and when
- [`KT-13028`](https://youtrack.jetbrains.com/issue/KT-13028) cast with star on on type with contravariant generic parameter makes the compiler crash
- [`KT-13339`](https://youtrack.jetbrains.com/issue/KT-13339) Type inference failed for synthetic Java property call on implicit smart cast receiver
- [`KT-13398`](https://youtrack.jetbrains.com/issue/KT-13398) "Type T is not a subtype of Any"
- [`KT-13683`](https://youtrack.jetbrains.com/issue/KT-13683) Type inference incorporation error when passed null into not-null parameter
- [`KT-13721`](https://youtrack.jetbrains.com/issue/KT-13721) Type inference fails when function arguments are involved
- [`KT-13725`](https://youtrack.jetbrains.com/issue/KT-13725) Type inference fails to infer type with array as a generic argument
- [`KT-13800`](https://youtrack.jetbrains.com/issue/KT-13800) Type inference fails when null passed as argument (related to common system)
- [`KT-13934`](https://youtrack.jetbrains.com/issue/KT-13934) Callable reference to companion object member via class name is not resolved
- [`KT-13964`](https://youtrack.jetbrains.com/issue/KT-13964) Unknown descriptor on compiling invoke operator call with generic parameter
- [`KT-13965`](https://youtrack.jetbrains.com/issue/KT-13965) Invoke operator called on extension property of generic type has incorrect parameter type
- [`KT-13992`](https://youtrack.jetbrains.com/issue/KT-13992) Incorrect TYPE_INFERENCE_UPPER_BOUND_VIOLATED
- [`KT-14101`](https://youtrack.jetbrains.com/issue/KT-14101) Type inference failed on null
- [`KT-14174`](https://youtrack.jetbrains.com/issue/KT-14174) Compiler can't infer type when it should be able to
- [`KT-14351`](https://youtrack.jetbrains.com/issue/KT-14351) Internal error with uninferred types for compiling complicated when expression
- [`KT-14460`](https://youtrack.jetbrains.com/issue/KT-14460) Smartcast isn't considered as necessary in the last expression of lambda
- [`KT-14463`](https://youtrack.jetbrains.com/issue/KT-14463) Missing MEMBER_PROJECTED_OUT error after smartcast with invoke
- [`KT-14499`](https://youtrack.jetbrains.com/issue/KT-14499) Type inference fails even if we specify `T` explicitly
- [`KT-14725`](https://youtrack.jetbrains.com/issue/KT-14725) kotlin generics makes agera's compiled repositories unusable
- [`KT-14803`](https://youtrack.jetbrains.com/issue/KT-14803) Weird smart cast absence in lazy delegate
- [`KT-14972`](https://youtrack.jetbrains.com/issue/KT-14972) Type inference failed: wrong common supertype for types with several subtypes
- [`KT-14980`](https://youtrack.jetbrains.com/issue/KT-14980) Type inference for nullable type in if statement
- [`KT-15155`](https://youtrack.jetbrains.com/issue/KT-15155) False "No cast needed" warning for conversion to star-projected type
- [`KT-15185`](https://youtrack.jetbrains.com/issue/KT-15185) Inference for coroutines not work in case where we have suspend function with new coroutine inside
- [`KT-15263`](https://youtrack.jetbrains.com/issue/KT-15263) Kotlin can't infer the type of a multiple bounded method
- [`KT-15389`](https://youtrack.jetbrains.com/issue/KT-15389) Type inference for coroutines in 1.1-M04
- [`KT-15394`](https://youtrack.jetbrains.com/issue/KT-15394) Support coercion to Unit for last statement for suspend lambda
- [`KT-15396`](https://youtrack.jetbrains.com/issue/KT-15396) Type inference for last statement in coroutine
- [`KT-15488`](https://youtrack.jetbrains.com/issue/KT-15488) Type inference not working on generic function returning generic type in lambda result
- [`KT-15648`](https://youtrack.jetbrains.com/issue/KT-15648) Better use of overladed functions with nullables
- [`KT-15922`](https://youtrack.jetbrains.com/issue/KT-15922) TYPE_INFERENCE_CONFLICTING_SUBSTITUTION for function reference
- [`KT-15923`](https://youtrack.jetbrains.com/issue/KT-15923) Internal error: empty intersection for types
- [`KT-16247`](https://youtrack.jetbrains.com/issue/KT-16247) Overload resolution ambiguity with intersection types and method references
- [`KT-16249`](https://youtrack.jetbrains.com/issue/KT-16249) Can't call generic method with overloaded method reference when non-generic overload exists
- [`KT-16421`](https://youtrack.jetbrains.com/issue/KT-16421) Remove @ParameterName annotation from diagnostic messages
- [`KT-16480`](https://youtrack.jetbrains.com/issue/KT-16480) Wrong "Type mismatch" for variable as function call
- [`KT-16591`](https://youtrack.jetbrains.com/issue/KT-16591) Type inferencing doesn't consider common base class of T of multiple Foo<T> function parameters
- [`KT-16678`](https://youtrack.jetbrains.com/issue/KT-16678) Overload resolution ambiguity on println()
- [`KT-16844`](https://youtrack.jetbrains.com/issue/KT-16844) Recursive dependency] (DeferredType) error on code generation.
- [`KT-16869`](https://youtrack.jetbrains.com/issue/KT-16869) Cannot infer type parameter S in fun <S, T : S?> f(x: S, g: (S) -> T): T
- [`KT-17018`](https://youtrack.jetbrains.com/issue/KT-17018) "Rewrite at slice LEXICAL_SCOPE" for 'when' with function reference inside lambda
- [`KT-17048`](https://youtrack.jetbrains.com/issue/KT-17048) Compilation exception: error type encountered in some combination of when/elvis with multiple inheritance
- [`KT-17340`](https://youtrack.jetbrains.com/issue/KT-17340) Type inference failed on overloaded method reference with expected KFunction
- [`KT-17386`](https://youtrack.jetbrains.com/issue/KT-17386) Smart cast on LHS of callable reference doesn't work if expected type is nullable
- [`KT-17487`](https://youtrack.jetbrains.com/issue/KT-17487) ReenteringLazyValueComputationException for interface with a subclass that has a property of interface type
- [`KT-17552`](https://youtrack.jetbrains.com/issue/KT-17552) Call completed not running for elvis as last expression in lambda
- [`KT-17799`](https://youtrack.jetbrains.com/issue/KT-17799) Smart cast doesn't work for callable reference (colon-colon operator)
- [`KT-17968`](https://youtrack.jetbrains.com/issue/KT-17968) More type inference problems with Streams.collect
- [`KT-17995`](https://youtrack.jetbrains.com/issue/KT-17995) No type mismatch with empty `when` expression inside lambda
- [`KT-18002`](https://youtrack.jetbrains.com/issue/KT-18002) Issue with star projection for generic type with recursive upper bound
- [`KT-18014`](https://youtrack.jetbrains.com/issue/KT-18014) Cannot use Comparator.comparing
- [`KT-18080`](https://youtrack.jetbrains.com/issue/KT-18080) Type inference failed for generic method reference argument
- [`KT-18192`](https://youtrack.jetbrains.com/issue/KT-18192) Type inference fails for some nested generic type structures
- [`KT-18207`](https://youtrack.jetbrains.com/issue/KT-18207) Unexpected attempt to smart cast causes compile error
- [`KT-18379`](https://youtrack.jetbrains.com/issue/KT-18379) Type inference failed although explicit specified
- [`KT-18401`](https://youtrack.jetbrains.com/issue/KT-18401) Type mismatch when inferring out type parameter (related to callable reference)
- [`KT-18481`](https://youtrack.jetbrains.com/issue/KT-18481) ReenteringLazyValueComputationException for recursive const declaration without explicit type
- [`KT-18541`](https://youtrack.jetbrains.com/issue/KT-18541) Prohibit "tailrec" modifier on open functions
- [`KT-18790`](https://youtrack.jetbrains.com/issue/KT-18790) function take the parameterized type with multi-bounds can't working 
- [`KT-19139`](https://youtrack.jetbrains.com/issue/KT-19139) Kotlin build error in Android Studio on << intent.putExtra(“string”, it.getString(“string”) >> inside "else if" block
- [`KT-19751`](https://youtrack.jetbrains.com/issue/KT-19751) Nested conditionals break type inference when multiple types are possible for the expression
- [`KT-19880`](https://youtrack.jetbrains.com/issue/KT-19880) An overload with nullable parameter is not resolved due to a smart cast attempt after assignment
- [`KT-19884`](https://youtrack.jetbrains.com/issue/KT-19884) type inference with type parameter
- [`KT-20226`](https://youtrack.jetbrains.com/issue/KT-20226) Return type 'Any' inferred for lambda with early return with integer literal in one of possible return values
- [`KT-20656`](https://youtrack.jetbrains.com/issue/KT-20656) Callable reference breaks smart cast for next statement
- [`KT-20734`](https://youtrack.jetbrains.com/issue/KT-20734) Invalid "Smart-cast impossible" when there are applicable extension and semi-applicable member
- [`KT-20817`](https://youtrack.jetbrains.com/issue/KT-20817) Coroutine builder type inference does not take smart casts into account (confusing)
- [`KT-21060`](https://youtrack.jetbrains.com/issue/KT-21060) ReenteringLazyValueComputationException during type inference
- [`KT-21396`](https://youtrack.jetbrains.com/issue/KT-21396) if-else with a branch of type Nothing fails to infer receiver type
- [`KT-21463`](https://youtrack.jetbrains.com/issue/KT-21463) Compiler doesn't take into accout a type parameter upper bound if a corresponding type argument is in projection
- [`KT-21607`](https://youtrack.jetbrains.com/issue/KT-21607) Type inference fails with intermediate function
- [`KT-21694`](https://youtrack.jetbrains.com/issue/KT-21694) Type inference failed for ObservableList
- [`KT-22012`](https://youtrack.jetbrains.com/issue/KT-22012) Kotlin type deduction does not handle Java's <? super T> constructs
- [`KT-22022`](https://youtrack.jetbrains.com/issue/KT-22022) ReenteringLazyValueComputationException when having cyclic references to static variables
- [`KT-22032`](https://youtrack.jetbrains.com/issue/KT-22032) Multiple type parameter bounds & smart casts
- [`KT-22043`](https://youtrack.jetbrains.com/issue/KT-22043) Report an error when comparing enum (==/!=/when) to any other incompatible type since 1.4
- [`KT-22070`](https://youtrack.jetbrains.com/issue/KT-22070) Type inference issue with platform types and SAM conversion
- [`KT-22474`](https://youtrack.jetbrains.com/issue/KT-22474) Type safety problem because of incorrect subtyping for intersection types
- [`KT-22636`](https://youtrack.jetbrains.com/issue/KT-22636) Anonymous function can be passed as a suspending one, failing at runtime
- [`KT-22723`](https://youtrack.jetbrains.com/issue/KT-22723) Inconsistent behavior of floating-point number comparisons
- [`KT-22775`](https://youtrack.jetbrains.com/issue/KT-22775) Type inference failed when yielding smartcasted not null value from `sequence`
- [`KT-22885`](https://youtrack.jetbrains.com/issue/KT-22885) Broken type safety with function variables caused by wrong subtyping check that includes intersection types
- [`KT-23141`](https://youtrack.jetbrains.com/issue/KT-23141) Cannot resolve between two functions when only one of them accepts nullable parameter
- [`KT-23156`](https://youtrack.jetbrains.com/issue/KT-23156) IDEA asking me to remove uninferable explicit type signature
- [`KT-23391`](https://youtrack.jetbrains.com/issue/KT-23391) Bogus type inference error with smart cast, synthetic property and star projection
- [`KT-23475`](https://youtrack.jetbrains.com/issue/KT-23475) Implicit invoke on property with generic functional return type resolves incorrectly
- [`KT-23482`](https://youtrack.jetbrains.com/issue/KT-23482) Incorrect frontend behaviour for not inferred type for generic property with function type as result
- [`KT-23677`](https://youtrack.jetbrains.com/issue/KT-23677) Incorrect error diagnostic for return types that coerced to 'Unit' inside lambda
- [`KT-23748`](https://youtrack.jetbrains.com/issue/KT-23748) Erroneous type mismatch when call with @Exact annotation depends on non-fixed type variable
- [`KT-23755`](https://youtrack.jetbrains.com/issue/KT-23755) Nested lambdas with nested types can cause false USELESS_CAST warning
- [`KT-23791`](https://youtrack.jetbrains.com/issue/KT-23791) Explicit type argument leads to an error, while candidate is picked correctly without it
- [`KT-23992`](https://youtrack.jetbrains.com/issue/KT-23992) Target prefixes for annotations on supertype list elements are not checked
- [`KT-24143`](https://youtrack.jetbrains.com/issue/KT-24143) Type-checking error for generic functions
- [`KT-24217`](https://youtrack.jetbrains.com/issue/KT-24217) Type inference for generic functions reference problem.
- [`KT-24237`](https://youtrack.jetbrains.com/issue/KT-24237) Uninferred type parameter on variable with multiple smart casts in elvis
- [`KT-24317`](https://youtrack.jetbrains.com/issue/KT-24317) Unnecessary non-null assertion inspection shown
- [`KT-24341`](https://youtrack.jetbrains.com/issue/KT-24341) Compiler exception instead of error about "upper bound violated"
- [`KT-24355`](https://youtrack.jetbrains.com/issue/KT-24355) Overload resolution ambiguity due to unstable smartcast
- [`KT-24458`](https://youtrack.jetbrains.com/issue/KT-24458) Cannot solve “Conditional branch result is implicitly cast to Any” with List
- [`KT-24493`](https://youtrack.jetbrains.com/issue/KT-24493) Incorrect error about Array<Nothing> unsupported
- [`KT-24886`](https://youtrack.jetbrains.com/issue/KT-24886) Type inference failure in an if-else_if-else
- [`KT-24918`](https://youtrack.jetbrains.com/issue/KT-24918) type inference not good enough for return type with involved if statement
- [`KT-24920`](https://youtrack.jetbrains.com/issue/KT-24920) Type inference failure when using RxJava compose
- [`KT-24993`](https://youtrack.jetbrains.com/issue/KT-24993) Inference for buildSequence/yield doesn't work for labeled lambdas
- [`KT-25063`](https://youtrack.jetbrains.com/issue/KT-25063) List of Arrays - wrong type inferred if nullable involved
- [`KT-25268`](https://youtrack.jetbrains.com/issue/KT-25268) Incorrect type inference (least upper bound of the types) in exhaustive when for Number
- [`KT-25294`](https://youtrack.jetbrains.com/issue/KT-25294) Reactors Mono map function with Kotlin
- [`KT-25306`](https://youtrack.jetbrains.com/issue/KT-25306) Make f(a = arrayOf(x)) equivalent to f(a = *arrayOf(x))
- [`KT-25342`](https://youtrack.jetbrains.com/issue/KT-25342) Type inference: lazy with generic fails to infer
- [`KT-25434`](https://youtrack.jetbrains.com/issue/KT-25434) Too eager smartcast cause type mismatch
- [`KT-25585`](https://youtrack.jetbrains.com/issue/KT-25585) "Rewrite at slice LEXICAL_SCOPE" for `if` expression with callable reference inside lambda
- [`KT-25656`](https://youtrack.jetbrains.com/issue/KT-25656) Short-hand array literal notation type inference fails in nested annotations
- [`KT-25675`](https://youtrack.jetbrains.com/issue/KT-25675) `return when` leads to compiler error
- [`KT-25721`](https://youtrack.jetbrains.com/issue/KT-25721) Type inference does not work in nested try catch
- [`KT-25827`](https://youtrack.jetbrains.com/issue/KT-25827) NullPointerException in setResultingSubstitutor with a generic base class
- [`KT-25841`](https://youtrack.jetbrains.com/issue/KT-25841) Collection type is inferred as Collection<Any> if the values returned from `map` function have an intersection common type
- [`KT-25942`](https://youtrack.jetbrains.com/issue/KT-25942) Pass a parameter to generic function with several upper bounds
- [`KT-26157`](https://youtrack.jetbrains.com/issue/KT-26157) Failed type inference for `Delegates.observable` parameter with a nullable property
- [`KT-26264`](https://youtrack.jetbrains.com/issue/KT-26264) No smart cast on implicit receiver extension function call
- [`KT-26638`](https://youtrack.jetbrains.com/issue/KT-26638) Check for repeatablilty of annotations doesn't take into account annotations with use-site target
- [`KT-26698`](https://youtrack.jetbrains.com/issue/KT-26698) OnlyInputTypes doesn't work in presence of upper bound constraint
- [`KT-26704`](https://youtrack.jetbrains.com/issue/KT-26704) Correct variable-as-function call fails to compile in case of function type dependent on variable type
- [`KT-27440`](https://youtrack.jetbrains.com/issue/KT-27440) Infer lambda parameter type before lambda analysis in common system
- [`KT-27464`](https://youtrack.jetbrains.com/issue/KT-27464) False negative type mismatch when a non-null assertion is used on a top-level `var` mutable variable
- [`KT-27606`](https://youtrack.jetbrains.com/issue/KT-27606) Incorrect type inference when common supertype of enum classes is involved
- [`KT-27722`](https://youtrack.jetbrains.com/issue/KT-27722) mapNotNull on List<Result<T>> fails if T is not subtype of Any
- [`KT-27781`](https://youtrack.jetbrains.com/issue/KT-27781) ReenteringLazyValueComputationException for anonymous object property referring to itself in overridden method
- [`KT-27799`](https://youtrack.jetbrains.com/issue/KT-27799) Prohibit references to reified type parameters in annotation arguments in local classes / anonymous objects
- [`KT-28083`](https://youtrack.jetbrains.com/issue/KT-28083) Nothing-call smart cast of a variable that a lambda returns does not affect the lambda's return type
- [`KT-28111`](https://youtrack.jetbrains.com/issue/KT-28111) Type inference fails for return type of lambda with constrained generic
- [`KT-28242`](https://youtrack.jetbrains.com/issue/KT-28242) Not-null assertion operator doesn't affect smartcast types
- [`KT-28264`](https://youtrack.jetbrains.com/issue/KT-28264) Resulting type of a safe call should produce type that is intersected with Any
- [`KT-28305`](https://youtrack.jetbrains.com/issue/KT-28305) "Error type encountered" with elvis operator, type argument and explicit `Any` type
- [`KT-28319`](https://youtrack.jetbrains.com/issue/KT-28319) "resultingDescriptor shouldn't be null" for a star projection base class
- [`KT-28334`](https://youtrack.jetbrains.com/issue/KT-28334) Smartcast doesn't work if original type is type with star projection and there was already another smartcast
- [`KT-28370`](https://youtrack.jetbrains.com/issue/KT-28370) Var not-null smartcasts are wrong if reassignments are used inside catch section (in try-catch) or try section (in try-finally)
- [`KT-28424`](https://youtrack.jetbrains.com/issue/KT-28424) Type annotations are not analyzed properly in several cases
- [`KT-28584`](https://youtrack.jetbrains.com/issue/KT-28584) Complex condition fails smart cast
- [`KT-28614`](https://youtrack.jetbrains.com/issue/KT-28614) "Error type encountered: UninferredParameterTypeConstructor"
- [`KT-28726`](https://youtrack.jetbrains.com/issue/KT-28726) Recursion in type inference causes ReenteringLazyValueComputationException
- [`KT-28837`](https://youtrack.jetbrains.com/issue/KT-28837) SAM conversion doesn't work for arguments of members imported from object
- [`KT-28873`](https://youtrack.jetbrains.com/issue/KT-28873) Function reference resolution ambiguity is not reported
- [`KT-28999`](https://youtrack.jetbrains.com/issue/KT-28999) Prohibit type parameters for anonymous objects
- [`KT-28951`](https://youtrack.jetbrains.com/issue/KT-28951) "Error type encountered" with elvis operator, type argument and explicit `Any` type
- [`KT-29014`](https://youtrack.jetbrains.com/issue/KT-29014) Unclear diagnostic for conditional branches coercion with multiple root interfaces and no common superclass
- [`KT-29079`](https://youtrack.jetbrains.com/issue/KT-29079) `ByteArray.let(::String)` can not compile as the callable reference is resolved before the outer call
- [`KT-29258`](https://youtrack.jetbrains.com/issue/KT-29258) Incorrect return type is used for analysis of an extension lambda (if its type is used as a generic parameter)
- [`KT-29330`](https://youtrack.jetbrains.com/issue/KT-29330) NI: Multiple duplicate error messages in IDE popup with lambda argument
- [`KT-29402`](https://youtrack.jetbrains.com/issue/KT-29402) Missed INLINE_FROM_HIGHER_PLATFORM diagnostic if inline function called from Derived class
- [`KT-29515`](https://youtrack.jetbrains.com/issue/KT-29515) type inference fails to infer T of KFunction0 for most types
- [`KT-29712`](https://youtrack.jetbrains.com/issue/KT-29712) Incorrect compiler warning, "No cast needed" for recursive type bound
- [`KT-29876`](https://youtrack.jetbrains.com/issue/KT-29876) ReenteringLazyValueComputationException with usage of property inside lambda during assignment to that property
- [`KT-29911`](https://youtrack.jetbrains.com/issue/KT-29911) Not-null smart cast fails inside inline lambda after safe call on generic variable with nullable upper bound
- [`KT-29943`](https://youtrack.jetbrains.com/issue/KT-29943) Callable reference resolution ambiguity between a property and a function is reported incorrectly in function calls expecting KProperty1
- [`KT-29949`](https://youtrack.jetbrains.com/issue/KT-29949) Cannot chose among overload as soon as nullable type is involved
- [`KT-30151`](https://youtrack.jetbrains.com/issue/KT-30151) Any instead of Number is inferred as common super type of Int and Double
- [`KT-30176`](https://youtrack.jetbrains.com/issue/KT-30176) Compiler can't infer correct type argument which blocks overload resolution
- [`KT-30240`](https://youtrack.jetbrains.com/issue/KT-30240) Can't infer intersection type for a type variable with several bounds
- [`KT-30278`](https://youtrack.jetbrains.com/issue/KT-30278) Retain star projections in typeOf
- [`KT-30394`](https://youtrack.jetbrains.com/issue/KT-30394) Different behaviour in type inferences (bug in the old inference) when cast of variable of nullable type parameter to not-null is used
- [`KT-30496`](https://youtrack.jetbrains.com/issue/KT-30496) Wrong infix generic extension function is chosen for `null` receiver
- [`KT-30550`](https://youtrack.jetbrains.com/issue/KT-30550) Suspend modifier on a functional type changes resolution of return type
- [`KT-30892`](https://youtrack.jetbrains.com/issue/KT-30892) IllegalStateException(UninferredParameterTypeConstructor) with return references to local function from if-else expression
- [`KT-30947`](https://youtrack.jetbrains.com/issue/KT-30947) No smart cast on accessing generic class member with upper-bounded type argument
- [`KT-31102`](https://youtrack.jetbrains.com/issue/KT-31102) Type mismatch in mixing lambda and callable reference
- [`KT-31151`](https://youtrack.jetbrains.com/issue/KT-31151) "IllegalStateException: Error type encountered" with elvis, when and inheritance
- [`KT-31219`](https://youtrack.jetbrains.com/issue/KT-31219) Type mismatch for delegated property depending on anonymous object
- [`KT-31290`](https://youtrack.jetbrains.com/issue/KT-31290) overload resolution ambiguity with Iterable<T>.mapTo()
- [`KT-31352`](https://youtrack.jetbrains.com/issue/KT-31352) JvmName can't eliminate platform declaration clash of annotated properties
- [`KT-31532`](https://youtrack.jetbrains.com/issue/KT-31532) Type inference for complex last expression in lambda
- [`KT-31540`](https://youtrack.jetbrains.com/issue/KT-31540) Change initialization order of default values for tail recursive optimized functions
- [`KT-31594`](https://youtrack.jetbrains.com/issue/KT-31594) No SETTER_PROJECTED_OUT diagnostic on synthetic  properties from Java
- [`KT-31630`](https://youtrack.jetbrains.com/issue/KT-31630) TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR for generic function with callable reference argument
- [`KT-31654`](https://youtrack.jetbrains.com/issue/KT-31654) False unnecessary non-null assertion (!!) warning inside yield call
- [`KT-31679`](https://youtrack.jetbrains.com/issue/KT-31679) NI: Unresolved reference with delegated property and anonymous object
- [`KT-31739`](https://youtrack.jetbrains.com/issue/KT-31739) Overload resolution ambiguity in generic function call with lambda arguments that take different parameter types
- [`KT-31923`](https://youtrack.jetbrains.com/issue/KT-31923) Outer finally block inserted before return instruction is not excluded from catch interval of inner try (without finally) block
- [`KT-31968`](https://youtrack.jetbrains.com/issue/KT-31968) New inference is using a more common system to infer types from nested elvis call than OI
- [`KT-31978`](https://youtrack.jetbrains.com/issue/KT-31978) NI: changed precedence of elvis operator relative to equality operator
- [`KT-32026`](https://youtrack.jetbrains.com/issue/KT-32026) Infer return type of @PolymorphicSignature method to void if no expected type is given
- [`KT-32087`](https://youtrack.jetbrains.com/issue/KT-32087) "Remove explicit type arguments" inspection when function creates a lambda that calls a function with type argument
- [`KT-32097`](https://youtrack.jetbrains.com/issue/KT-32097) NI: NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE when using awaitClose in channelFlow builder
- [`KT-32098`](https://youtrack.jetbrains.com/issue/KT-32098) Kotlin 1.3.40 type inference mismatch between the IDE and the compiler
- [`KT-32151`](https://youtrack.jetbrains.com/issue/KT-32151) Return arguments of lambda are resolved in common system while in OI aren't
- [`KT-32165`](https://youtrack.jetbrains.com/issue/KT-32165) Cannot use 'Nothing' as reified type parameter,
- [`KT-32196`](https://youtrack.jetbrains.com/issue/KT-32196) Inconsistency between compiler and inspection with mapNotNull
- [`KT-32203`](https://youtrack.jetbrains.com/issue/KT-32203) NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE warning false-positive
- [`KT-32249`](https://youtrack.jetbrains.com/issue/KT-32249) New inference breaks generic property delegate resolution
- [`KT-32250`](https://youtrack.jetbrains.com/issue/KT-32250) New Type Inference fails for nullable field
- [`KT-32262`](https://youtrack.jetbrains.com/issue/KT-32262) Type inference failed in presence of @JvmSuppressWildcards
- [`KT-32267`](https://youtrack.jetbrains.com/issue/KT-32267) NI: "Overload resolution ambiguity. All these functions match." with KFunction
- [`KT-32284`](https://youtrack.jetbrains.com/issue/KT-32284) False positive "Redundant lambda arrow" with extension function returning generic with lambda type argument
- [`KT-32290`](https://youtrack.jetbrains.com/issue/KT-32290) New Inference: old inference fails on nullable lambda from if-expression
- [`KT-32306`](https://youtrack.jetbrains.com/issue/KT-32306) False positive `Remove explicit type arguments` when using generic return argument from lambda for delegated property
- [`KT-32358`](https://youtrack.jetbrains.com/issue/KT-32358) NI: Smart cast doesn't work with inline function after elvis operator
- [`KT-32383`](https://youtrack.jetbrains.com/issue/KT-32383) NI: listOf(…) infers to invalid type if different enums implementing the same interface are involved
- [`KT-32397`](https://youtrack.jetbrains.com/issue/KT-32397) OI can't infer types when there are different lower and upper bounds in common system
- [`KT-32399`](https://youtrack.jetbrains.com/issue/KT-32399) OI can't choose candidate with different lower and upper bounds
- [`KT-32425`](https://youtrack.jetbrains.com/issue/KT-32425) No coercion to Unit by a return type of callable expression argument
- [`KT-32431`](https://youtrack.jetbrains.com/issue/KT-32431) Nothing inferred for upper bound in parametric class
- [`KT-32449`](https://youtrack.jetbrains.com/issue/KT-32449) IDE fails to report error:  Expected type mismatch
- [`KT-32462`](https://youtrack.jetbrains.com/issue/KT-32462) NI: "AssertionError: No resolved call" with callable reference
- [`KT-32497`](https://youtrack.jetbrains.com/issue/KT-32497) Compiler Type inference fails when inferring type parameters ("Generics") - IDE inference works
- [`KT-32507`](https://youtrack.jetbrains.com/issue/KT-32507) IntelliJ Kotlin plugin not recognizing smart cast to non-nullable type
- [`KT-32501`](https://youtrack.jetbrains.com/issue/KT-32501) type inference should infer nullable type if non-nullable doesn't work
- [`KT-32527`](https://youtrack.jetbrains.com/issue/KT-32527) Cast required for Sequence.map when mapping between disjoint types
- [`KT-32548`](https://youtrack.jetbrains.com/issue/KT-32548) OVERLOAD_RESOLUTION_AMBIGUITY with platform generic types
- [`KT-32595`](https://youtrack.jetbrains.com/issue/KT-32595) NI: Overload resolution ambiguity for member function with type parameter with upper bounds and lambda parameter
- [`KT-32598`](https://youtrack.jetbrains.com/issue/KT-32598) Kotlin 1.3.41 type inference problem
- [`KT-32654`](https://youtrack.jetbrains.com/issue/KT-32654) Non-applicable call for builder inference with coroutines
- [`KT-32655`](https://youtrack.jetbrains.com/issue/KT-32655) Kotlin compiler and IDEA plugin disagree on type
- [`KT-32686`](https://youtrack.jetbrains.com/issue/KT-32686) New type inference algorithm infers wrong type when return type of `when` expression has a type parameter, and it's not specified in all cases
- [`KT-32788`](https://youtrack.jetbrains.com/issue/KT-32788) No smartcast for an implicit "this" in old inference
- [`KT-32792`](https://youtrack.jetbrains.com/issue/KT-32792) Bug in new type inference algorithm for Android Studio
- [`KT-32800`](https://youtrack.jetbrains.com/issue/KT-32800) Problem with inlined getValue in Delegates, IDEA does not detect problem, but code doesn't compile
- [`KT-32802`](https://youtrack.jetbrains.com/issue/KT-32802) Odd behaviour with smart-casting in exception block
- [`KT-32850`](https://youtrack.jetbrains.com/issue/KT-32850) Invalid suggestion in Kotlin code
- [`KT-32866`](https://youtrack.jetbrains.com/issue/KT-32866) tests in Atrium do not compile with new type inference
- [`KT-33012`](https://youtrack.jetbrains.com/issue/KT-33012) Proper capturing of star projections with recursive types
- [`KT-33102`](https://youtrack.jetbrains.com/issue/KT-33102) Fake overrides aren't created for properties
- [`KT-33152`](https://youtrack.jetbrains.com/issue/KT-33152) Type inference fails when lambda with generic argument type is present
- [`KT-33166`](https://youtrack.jetbrains.com/issue/KT-33166) NI: TYPE_MISMATCH for specific case with `when` expression and one branch throwing exception
- [`KT-33171`](https://youtrack.jetbrains.com/issue/KT-33171) TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER with object convention invoke direct call on property delegation
- [`KT-33240`](https://youtrack.jetbrains.com/issue/KT-33240) Generated overloads for @JvmOverloads on open methods should be final
- [`KT-33545`](https://youtrack.jetbrains.com/issue/KT-33545) NI: Error type encountered: NonFixed: TypeVariable(T) (StubType)
- [`KT-33988`](https://youtrack.jetbrains.com/issue/KT-33988) Low priority candidates doesn't match when others fail
- [`KT-34128`](https://youtrack.jetbrains.com/issue/KT-34128) Internal compiler error ReenteringLazyValueComputationException in the absence of right parenthesis
- [`KT-34140`](https://youtrack.jetbrains.com/issue/KT-34140) Function with contract doesn't smartcast return value in lambda
- [`KT-34314`](https://youtrack.jetbrains.com/issue/KT-34314) There is no an error diagnostic about type inference failed (impossible to infer type parameters) on callable references
- [`KT-34335`](https://youtrack.jetbrains.com/issue/KT-34335) A lambda argument without a type specifier is inferred to Nothing, if it's passed to a function, declaration of which contains vararg (e.g. Any)
- [`KT-34501`](https://youtrack.jetbrains.com/issue/KT-34501) if-expression infers `Any` when `Number?` is expected
- [`KT-34708`](https://youtrack.jetbrains.com/issue/KT-34708) Couldn't transform method node: emit$$forInline (try-catch in a Flow.map)
- [`KT-34729`](https://youtrack.jetbrains.com/issue/KT-34729) NI: type mismatch error is missed for generic higher-order functions
- [`KT-34830`](https://youtrack.jetbrains.com/issue/KT-34830) Type inference fails when using an extension method with "identical JVM signature" as method reference
- [`KT-34857`](https://youtrack.jetbrains.com/issue/KT-34857) "Illegal resolved call to variable with invoke" with operator resolved to extension property `invoke`
- [`KT-34891`](https://youtrack.jetbrains.com/issue/KT-34891) CompilationException: Failed to generate expression: KtLambdaExpression (Error type encountered)
- [`KT-34925`](https://youtrack.jetbrains.com/issue/KT-34925) MPP, IDE: False positive warning NO_REFLECTION_IN_CLASS_PATH in common code
- [`KT-35020`](https://youtrack.jetbrains.com/issue/KT-35020) "Type checking has run into a recursive problem" for overloaded generic function implemented using expression body syntax
- [`KT-35064`](https://youtrack.jetbrains.com/issue/KT-35064) NI: The new inference has overload resolution ambiguity on passing a callable reference for extension functions (> 1 candidates)
- [`KT-35207`](https://youtrack.jetbrains.com/issue/KT-35207) Incorrect generic signature in annotations when KClass is used as a generic parameter
- [`KT-35210`](https://youtrack.jetbrains.com/issue/KT-35210) NI: OnlyInputTypes check fails for types with captured ones
- [`KT-35213`](https://youtrack.jetbrains.com/issue/KT-35213) NI: overload resolution ambiguity for callable reference with defined LHS
- [`KT-35226`](https://youtrack.jetbrains.com/issue/KT-35226) Forbid spread operator in signature-polymorphic calls
- [`KT-35306`](https://youtrack.jetbrains.com/issue/KT-35306) `Non-applicable call for builder inference` for nested builder functions which return generic types that are wrapped.
- [`KT-35337`](https://youtrack.jetbrains.com/issue/KT-35337) IllegalStateException: Failed to generate expression: KtLambdaExpression
- [`KT-35398`](https://youtrack.jetbrains.com/issue/KT-35398) NI, IDE: Duplicate warning message JAVA_CLASS_ON_COMPANION in argument position
- [`KT-35469`](https://youtrack.jetbrains.com/issue/KT-35469) Change behavior of signature-polymorphic calls to methods with a single vararg parameter, to avoid wrapping the argument into another array
- [`KT-35487`](https://youtrack.jetbrains.com/issue/KT-35487) Type safety problem because of lack of captured conversion against nullable type argument
- [`KT-35494`](https://youtrack.jetbrains.com/issue/KT-35494) NI: Multiple duplicate error diagnostics (in IDE popup) with NULL_FOR_NONNULL_TYPE
- [`KT-35514`](https://youtrack.jetbrains.com/issue/KT-35514) Type inference failure with `out` type and if-else inside lambda
- [`KT-35517`](https://youtrack.jetbrains.com/issue/KT-35517) TYPE_MISMATCH error duplication for not Boolean condition in if-expression
- [`KT-35535`](https://youtrack.jetbrains.com/issue/KT-35535) Illegal callable reference receiver allowed in new inference
- [`KT-35578`](https://youtrack.jetbrains.com/issue/KT-35578) Diagnostics are sometimes duplicated
- [`KT-35602`](https://youtrack.jetbrains.com/issue/KT-35602) NI doesn't approximate star projections properly for self types
- [`KT-35658`](https://youtrack.jetbrains.com/issue/KT-35658) NI: Common super type between Inv<A!>, Inv<A?> and Inv<A> is Inv<out A?>, not Inv<A!> (as old inference)
- [`KT-35668`](https://youtrack.jetbrains.com/issue/KT-35668) NI: the lack of fixing to Nothing problem (one of the bugs: the lack of smartcast through cast of Nothing? to Nothing after elvis)
- [`KT-35679`](https://youtrack.jetbrains.com/issue/KT-35679) Type safety problem because several equal type variables are instantiated with a different types
- [`KT-35684`](https://youtrack.jetbrains.com/issue/KT-35684) NI: "IllegalStateException: Expected some types" from builder-inference about intersecting empty types on trivial code
- [`KT-35814`](https://youtrack.jetbrains.com/issue/KT-35814) Inference fails to infer common upper type for java types
- [`KT-35834`](https://youtrack.jetbrains.com/issue/KT-35834) Do not declare checked exceptions in JVM bytecode when using delegation to Kotlin interfaces
- [`KT-35920`](https://youtrack.jetbrains.com/issue/KT-35920) NI allows callable references prohibited in old inference
- [`KT-35943`](https://youtrack.jetbrains.com/issue/KT-35943) NI: IllegalStateException: No error about uninferred type parameter
- [`KT-35945`](https://youtrack.jetbrains.com/issue/KT-35945) Using Polymorphism and Type Inference the compiler fails having a Type with Type with Type
- [`KT-35992`](https://youtrack.jetbrains.com/issue/KT-35992) Wrong overload resolution with explicit type arguments if KFunction and type parameter once nullable and once non-nullable
- [`KT-36001`](https://youtrack.jetbrains.com/issue/KT-36001) "IllegalStateException: Error type encountered" with elvis in generic function
- [`KT-36002`](https://youtrack.jetbrains.com/issue/KT-36002) KotlinFrontEndException: Exception while analyzing expression
- [`KT-36065`](https://youtrack.jetbrains.com/issue/KT-36065) Type inference failed for generic function invoked on implicitly nullable variable created from Java
- [`KT-36066`](https://youtrack.jetbrains.com/issue/KT-36066) Type inference problem in compiler
- [`KT-36101`](https://youtrack.jetbrains.com/issue/KT-36101) False positive IMPLICIT_NOTHING_AS_TYPE_PARAMETER with suspend lambdas
- [`KT-36146`](https://youtrack.jetbrains.com/issue/KT-36146) Drop support of language version 1.0/1.1, deprecate language version 1.2
- [`KT-36192`](https://youtrack.jetbrains.com/issue/KT-36192) Redundant smart cast for overloaded functions
- [`KT-36202`](https://youtrack.jetbrains.com/issue/KT-36202) NI: false positive "NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE" with elvis operator in lambda
- [`KT-36220`](https://youtrack.jetbrains.com/issue/KT-36220) NI: false positive NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE if one use cannot resolve
- [`KT-36221`](https://youtrack.jetbrains.com/issue/KT-36221) NI: UnsupportedOperationException: "no descriptor for type constructor" with vararg parameter when `set` or `get` methods of element passed as function reference
- [`KT-36264`](https://youtrack.jetbrains.com/issue/KT-36264) NI: unstable smart cast changes results of call resolution
- [`KT-36279`](https://youtrack.jetbrains.com/issue/KT-36279) OI: Type inference fails when omitting explicit name of the lambda parameter
- [`KT-36298`](https://youtrack.jetbrains.com/issue/KT-36298) Failure to resolve return type of extension property on function literal
- [`KT-36317`](https://youtrack.jetbrains.com/issue/KT-36317) TYPE_MISMATCH diagnostic error duplication for not Boolean condition in do-while-loop-statement
- [`KT-36338`](https://youtrack.jetbrains.com/issue/KT-36338) Forbid IR backend usage with unsupported language / API version
- [`KT-36371`](https://youtrack.jetbrains.com/issue/KT-36371) Incorrect null check with NI and builder inference in generic context
- [`KT-36644`](https://youtrack.jetbrains.com/issue/KT-36644) Stop discrimination of prerelease compiler version in Kotlin plugin
- [`KT-36745`](https://youtrack.jetbrains.com/issue/KT-36745) NI: "Expected some types" with unresolved class reference
- [`KT-36776`](https://youtrack.jetbrains.com/issue/KT-36776) Treat special constructions (if, when, try) as a usual calls when there is expected type
- [`KT-36818`](https://youtrack.jetbrains.com/issue/KT-36818) NI: the lack of smartcast from Nothing returning "when" branch
- [`KT-37123`](https://youtrack.jetbrains.com/issue/KT-37123) Cycling lambda signature errors in Observable.combineLatest
- [`KT-37146`](https://youtrack.jetbrains.com/issue/KT-37146) Type inference can't resolve plus operator
- [`KT-37189`](https://youtrack.jetbrains.com/issue/KT-37189) Wrong type inferred with class with two types as argument
- [`KT-37295`](https://youtrack.jetbrains.com/issue/KT-37295) NI: Passing function with wrong returning type by reference is not shown as error
- [`KT-37345`](https://youtrack.jetbrains.com/issue/KT-37345) Wrong non-null type assertion when using Sequence and yield
- [`KT-37429`](https://youtrack.jetbrains.com/issue/KT-37429) Type inference failed: Not enough information to infer parameter with java streams
- [`KT-37480`](https://youtrack.jetbrains.com/issue/KT-37480) "None of the following candidates can be called" when compiling but no error in IDEA

### IDE

- [`KT-33573`](https://youtrack.jetbrains.com/issue/KT-33573) IDE runs platform-specific checkers on common code even if the project doesn't target the corresponding platform
- [`KT-35823`](https://youtrack.jetbrains.com/issue/KT-35823) IDE settings: deprecated values of language / API version look like regular ones
- [`KT-35871`](https://youtrack.jetbrains.com/issue/KT-35871) NPE in LightMethodBuilder
- [`KT-36034`](https://youtrack.jetbrains.com/issue/KT-36034) Kotlin 1.3.70 creates file `kotlinCodeInsightSettings.xml` with user-level settings under .idea
- [`KT-36084`](https://youtrack.jetbrains.com/issue/KT-36084) "Join lines" should remove trailing comma
- [`KT-36460`](https://youtrack.jetbrains.com/issue/KT-36460) IDE highlighting: Undo on inspection breaks analysis inside top-level property initializer
- [`KT-36712`](https://youtrack.jetbrains.com/issue/KT-36712) Use new annotation highlighting API
- [`KT-36917`](https://youtrack.jetbrains.com/issue/KT-36917) Caret has incorrect position after pressing enter on line with named argument

### IDE. Code Style, Formatting

- [`KT-36387`](https://youtrack.jetbrains.com/issue/KT-36387) Formatter: "Chained Function Calls" formats property chains
- [`KT-36393`](https://youtrack.jetbrains.com/issue/KT-36393) Incorrect trailing comma insertion for boolean operator expression
- [`KT-36466`](https://youtrack.jetbrains.com/issue/KT-36466) Formatter: "Chained Function Calls" with "Wrap first call" wrap single method call

### IDE. Completion

- [`KT-16531`](https://youtrack.jetbrains.com/issue/KT-16531) Error type displayed in completion for the result of buildSequence
- [`KT-32178`](https://youtrack.jetbrains.com/issue/KT-32178) Autocompletion of 'suspend' should not add the 'fun' keyword when writing a function type
- [`KT-34582`](https://youtrack.jetbrains.com/issue/KT-34582) Remove kotlin.coroutines.experimental from autocompletion
- [`KT-35258`](https://youtrack.jetbrains.com/issue/KT-35258) Completion problems with enabled `MixedNamedArgumentsInTheirOwnPosition` feature

### IDE. Debugger

- [`KT-12016`](https://youtrack.jetbrains.com/issue/KT-12016) Step over inside inline function lambda argument steps into inline function body
- [`KT-14296`](https://youtrack.jetbrains.com/issue/KT-14296) Can't step over inlined functions of iterator
- [`KT-14869`](https://youtrack.jetbrains.com/issue/KT-14869) Debugger: always steps into inlined lambda after returning from function
- [`KT-15652`](https://youtrack.jetbrains.com/issue/KT-15652) Step over inline function call stops at Thread.dispatchUncaughtException() in case of exceptions
- [`KT-34905`](https://youtrack.jetbrains.com/issue/KT-34905) Debugger: "Step over" steps into inline function body if lambda call splitted on several lines
- [`KT-35354`](https://youtrack.jetbrains.com/issue/KT-35354) ClassCastException in evaluate window

### IDE. Gradle. Script

- [`KT-36703`](https://youtrack.jetbrains.com/issue/KT-36703) .gradle.kts: Change text for out of project scripts

### IDE. Hints

- [`KT-37537`](https://youtrack.jetbrains.com/issue/KT-37537) IDE is missing or swallowing keystrokes when hint popups are displayed

### IDE. Hints. Parameter Info

- [`KT-14523`](https://youtrack.jetbrains.com/issue/KT-14523) Weird parameter info tooltip for map.getValue type arguments

### IDE. Inspections and Intentions

#### New Features

- [`KT-33384`](https://youtrack.jetbrains.com/issue/KT-33384) Intention to switch between single-line/multi-line lambda
- [`KT-34690`](https://youtrack.jetbrains.com/issue/KT-34690) Support intention `Convert lambda to reference` for qualified references
- [`KT-35639`](https://youtrack.jetbrains.com/issue/KT-35639) False negative inspection "redundant internal modifier" inside private class
- [`KT-36256`](https://youtrack.jetbrains.com/issue/KT-36256) Implement migration for WarningOnMainUnusedParameter
- [`KT-36257`](https://youtrack.jetbrains.com/issue/KT-36257) Implement migration for ProhibitRepeatedUseSiteTargetAnnotations
- [`KT-36258`](https://youtrack.jetbrains.com/issue/KT-36258) Implement migration for ProhibitUseSiteTargetAnnotationsOnSuperTypes
- [`KT-36260`](https://youtrack.jetbrains.com/issue/KT-36260) Implement migration for ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses
- [`KT-36261`](https://youtrack.jetbrains.com/issue/KT-36261) Implement migration for ProhibitTypeParametersForLocalVariables
- [`KT-36262`](https://youtrack.jetbrains.com/issue/KT-36262) Implement migration for RestrictReturnStatementTarget

#### Fixes

- [`KT-14001`](https://youtrack.jetbrains.com/issue/KT-14001) "Convert lambda to reference" results in failed type inference
- [`KT-14781`](https://youtrack.jetbrains.com/issue/KT-14781) Import of aliased type is inserted when deprecation replacement contains typealias
- [`KT-16907`](https://youtrack.jetbrains.com/issue/KT-16907) "Convert to lambda reference" intention is erroneously shown for suspending lambda parameters, producing bad code
- [`KT-24869`](https://youtrack.jetbrains.com/issue/KT-24869) False positive inspection "Redundant 'suspend' modifier"
- [`KT-24987`](https://youtrack.jetbrains.com/issue/KT-24987) Implicit (unsafe) cast from dynamic to DONT_CARE
- [`KT-27511`](https://youtrack.jetbrains.com/issue/KT-27511) "Remove explicit type arguments" suggestion creates incorrect code
- [`KT-28415`](https://youtrack.jetbrains.com/issue/KT-28415) False positive inspection "Remove explicit type arguments" with a callable reference
- [`KT-30831`](https://youtrack.jetbrains.com/issue/KT-30831) False positive `Remove explicit type arguments` with generic type constructor with init block
- [`KT-31050`](https://youtrack.jetbrains.com/issue/KT-31050) False positive "Boolean literal argument without parameter name" inspection using expect class
- [`KT-31559`](https://youtrack.jetbrains.com/issue/KT-31559) Type inference failed: Not enough information to infer parameter K
- [`KT-32093`](https://youtrack.jetbrains.com/issue/KT-32093) NI: IDE suggests to use property access syntax instead of getter method
- [`KT-33098`](https://youtrack.jetbrains.com/issue/KT-33098) False positive "Remove explicit type arguments" with "Enable NI for IDE" and Java generic class constructor invocation with nullable argument
- [`KT-33685`](https://youtrack.jetbrains.com/issue/KT-33685) ReplaceWith does not add type parameter for function taking generic lambda with receiver
- [`KT-34511`](https://youtrack.jetbrains.com/issue/KT-34511) kotlin.KotlinNullPointerException after using intention Replace Java Map.forEach with Kotlin's forEach for Map with Pairs as keys
- [`KT-34686`](https://youtrack.jetbrains.com/issue/KT-34686) False positive "Constructor parameter is never used as a property" if property is used as a reference
- [`KT-35451`](https://youtrack.jetbrains.com/issue/KT-35451) Type inference fails after applying false positive inspection "Remove explicit type arguments "
- [`KT-35475`](https://youtrack.jetbrains.com/issue/KT-35475) Applying intention "Redundant curly braces in string template" for label references change semantic
- [`KT-35528`](https://youtrack.jetbrains.com/issue/KT-35528) Intention `Replace 'when' with 'if'` produces wrong code if expression subject is a variable declaration
- [`KT-35588`](https://youtrack.jetbrains.com/issue/KT-35588) Applying "Lift assignment out of 'if'" for if statement that has lambda plus assignment and 'return' leads to type mismatch
- [`KT-35604`](https://youtrack.jetbrains.com/issue/KT-35604) Too long quickfix message for "modifier 'open' is not applicable to 'companion object'"
- [`KT-35648`](https://youtrack.jetbrains.com/issue/KT-35648) False negative intention "Remove argument name" on named positional arguments
- [`KT-36160`](https://youtrack.jetbrains.com/issue/KT-36160) False positive "Constructor has non-null self reference parameter" with vararg parameter of class
- [`KT-36171`](https://youtrack.jetbrains.com/issue/KT-36171) intention "Replace 'get' call with indexing operator" works incorrectly with spread operator
- [`KT-36255`](https://youtrack.jetbrains.com/issue/KT-36255) Implement migration tool for 1.4
- [`KT-36357`](https://youtrack.jetbrains.com/issue/KT-36357) "Lift assignment out of 'if'" breaks code for oneliner function
- [`KT-36360`](https://youtrack.jetbrains.com/issue/KT-36360) OI: False positive "remove explicit type arguments" for SequenceScope leads to compiler failure with "Type inference failed"
- [`KT-36369`](https://youtrack.jetbrains.com/issue/KT-36369) "To raw string literal" intention is not available if string content starts with newline symbol \n

### IDE. Multiplatform

- [`KT-36978`](https://youtrack.jetbrains.com/issue/KT-36978) Infinite "org.jetbrains.kotlin.idea.caches.resolve.KotlinIdeaResolutionException: Kotlin resolution encountered a problem while analyzing KtFile" exceptions in hierarchical multiplatform projects

### IDE. Navigation

- [`KT-30628`](https://youtrack.jetbrains.com/issue/KT-30628) Navigation from Java sources to calls on Kotlin function annotated with @JvmOverloads that have optional arguments omitted leads to decompiled code

### IDE. Refactorings

#### New Features

- [`KT-26999`](https://youtrack.jetbrains.com/issue/KT-26999) Inspection for unused main parameter in Kotlin 1.3
- [`KT-33339`](https://youtrack.jetbrains.com/issue/KT-33339) Refactor / Move is disabled for Kotlin class selected together with Kotlin file

#### Fixes

- [`KT-22131`](https://youtrack.jetbrains.com/issue/KT-22131) "Extract method" refactoring treats smart casted variables as non-local
- [`KT-24615`](https://youtrack.jetbrains.com/issue/KT-24615) "Extract property" generates useless extension property
- [`KT-26047`](https://youtrack.jetbrains.com/issue/KT-26047) Refactor -> Rename: Overridden method renaming in generic class doesn't rename base method
- [`KT-26248`](https://youtrack.jetbrains.com/issue/KT-26248) "Refactor -> Inline variable" breaks callable references
- [`KT-31401`](https://youtrack.jetbrains.com/issue/KT-31401) Refactor / Inlining function reference in Java method with Runnable argument produces compile error
- [`KT-33709`](https://youtrack.jetbrains.com/issue/KT-33709) Inline variable: NONE_APPLICABLE with overloaded generic Java method with SAM conversion
- [`KT-34190`](https://youtrack.jetbrains.com/issue/KT-34190) Inline Function: False positive error "not at the end of the body" with anonymous object
- [`KT-35235`](https://youtrack.jetbrains.com/issue/KT-35235) Unable to move multiple class files to a different package
- [`KT-35463`](https://youtrack.jetbrains.com/issue/KT-35463) ClassCastException during moving .kt file with one single class to the other folder in Android Studio
- [`KT-36312`](https://youtrack.jetbrains.com/issue/KT-36312) Refactor Move refactoring to get it ready for MPP-related fixes

### IDE. Run Configurations

- [`KT-34503`](https://youtrack.jetbrains.com/issue/KT-34503) "Nothing here" is shown as a drop-down list for "Run test" gutter icon for a multiplatform test with expect/actual parts in platform-agnostic code
- [`KT-36093`](https://youtrack.jetbrains.com/issue/KT-36093) Running Gradle java tests are broken for Gradle older than 4.0 (Could not set unknown property 'testClassesDirs' for task ':nonJvmTestIdeSupport')

### JavaScript

- [`KT-23284`](https://youtrack.jetbrains.com/issue/KT-23284) Reified type arguments aren't substituted when invoking inline val value

### Libraries

- [`KT-26654`](https://youtrack.jetbrains.com/issue/KT-26654) Remove deprecated 'mod' operators
- [`KT-27856`](https://youtrack.jetbrains.com/issue/KT-27856) Add contracts to Timing.kt lambdas
- [`KT-28356`](https://youtrack.jetbrains.com/issue/KT-28356) Fail fast in Regex.findAll on an invalid startIndex
- [`KT-29748`](https://youtrack.jetbrains.com/issue/KT-29748) Declare kotlin.reflect.KType in kotlin-stdlib-common
- [`KT-30360`](https://youtrack.jetbrains.com/issue/KT-30360) Deprecate conversions of floating point types to integral types lesser than Int
- [`KT-32855`](https://youtrack.jetbrains.com/issue/KT-32855) KTypeParameter is available in common code, but not available in Native
- [`KT-35216`](https://youtrack.jetbrains.com/issue/KT-35216) <T : AutoCloseable?, R> T.use and <T : Closeable?, R> T.use should have contracts
- [`KT-36082`](https://youtrack.jetbrains.com/issue/KT-36082) JS Regex.find does not throw IndexOutOfBoundsException on invalid start index
- [`KT-36083`](https://youtrack.jetbrains.com/issue/KT-36083) Extract kotlin.coroutines.experimental.* packages to a separate compatibility artifact

### Reflection

- [`KT-30071`](https://youtrack.jetbrains.com/issue/KT-30071) Implement KTypeProjection.toString
- [`KT-35991`](https://youtrack.jetbrains.com/issue/KT-35991) Embed Proguard/R8 rules in kotlin-reflect artifact jar

### Tools. CLI

- [`KT-28475`](https://youtrack.jetbrains.com/issue/KT-28475) java.sql module not available in kotlinc scripts on Java 9+ due to use of Bootstrap Classloader

### Tools. Gradle. JS

- [`KT-34989`](https://youtrack.jetbrains.com/issue/KT-34989) NodeJs is not re-downloaded if the node binary gets accidentally deleted
- [`KT-35465`](https://youtrack.jetbrains.com/issue/KT-35465) Gradle, JS: Gradle tooling for IR compiler
- [`KT-36472`](https://youtrack.jetbrains.com/issue/KT-36472) Kotlin/JS Gradle plugin iterates over all Gradle tasks, triggering their lazy configuration
- [`KT-36488`](https://youtrack.jetbrains.com/issue/KT-36488) Gradle, JS, IR: Publishing from JS plugin

### Tools. Gradle. Multiplatform

- [`KT-37264`](https://youtrack.jetbrains.com/issue/KT-37264) In intermediate common source sets, internals are not visible from their dependsOn source sets during Gradle build

### Tools. Gradle. Native

- [`KT-36804`](https://youtrack.jetbrains.com/issue/KT-36804) In a project with Kotlin/Native targets and kotlinx.serialization, IDE import fails: Could not create task '...'. / Cannot change dependencies of configuration after it has been resolved.

### Tools. J2K

- [`KT-20120`](https://youtrack.jetbrains.com/issue/KT-20120) J2K: Java 9 `forRemoval` and `since` methods of `Deprecated` are not processed

### Tools. Scripts

- [`KT-35414`](https://youtrack.jetbrains.com/issue/KT-35414) Switch for `-Xexpression` to `-expression`/`-e` cli argument syntax for JVM cli compiler in 1.4

