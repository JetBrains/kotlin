## `Source session for module <main>`

### Call 1

```
JavaBox#(Null(null))
```

#### Candidate 1: `FirConstructorSymbol /JavaBox.JavaBox` --- `constructor<T : Any!>(b: T!): JavaBox<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirRegularClassSymbol JavaBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Nothing? <: TypeVariable(T)!` _from Argument Null(null)_
    1. `kotlin/Nothing? <: TypeVariable(T)`

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
   	false ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 2

```
R|<local>/x|.foo6#<R|kotlin/String?|>(R?C|/JavaBox.JavaBox|(Null(null)))
```

#### Candidate 1: `FirNamedFunctionSymbol /GenericFunWithAnnotation.foo6` --- `fun <X : Any!> foo6(@NotNull() a: @EnhancedNullability JavaBox<@EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  X & Any>): Unit`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(X)` for `FirNamedFunctionSymbol /GenericFunWithAnnotation.foo6`s parameter 0
2. `TypeVariable(X) == ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) kotlin/String!` _from TypeParameter R|kotlin/String?|_

##### Resolution Stages > CheckArguments:

1. `JavaBox<TypeVariable(T)> <: @EnhancedNullability JavaBox<@EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(X) & Any>` _from Argument R?C|/JavaBox.JavaBox|(Null(null))_
    1. `TypeVariable(T) <: TypeVariable(X)`
    2. `TypeVariable(T) <: @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(X) & Any`
    3. `@EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(X) & Any <: TypeVariable(T)`
    4. `TypeVariable(X) <: TypeVariable(T)?`
2. Combine `TypeVariable(T) <: TypeVariable(X)` with `TypeVariable(X) == ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) kotlin/String!`
    1. `TypeVariable(T) <: ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) kotlin/String!`
3. Combine `kotlin/Nothing? <: TypeVariable(T)` with `TypeVariable(T) <: @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(X) & Any`
    1. `kotlin/Nothing? <: TypeVariable(X)`
4. Combine `TypeVariable(T) <: @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(X) & Any` with `@EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(X) & Any <: TypeVariable(T)`
    1. `TypeVariable(T) == @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(X) & Any`
5. Combine `TypeVariable(X) == ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) kotlin/String!` with `TypeVariable(X) <: TypeVariable(T)?`
    1. `kotlin/String <: TypeVariable(T)`
6. Combine `TypeVariable(T) <: TypeVariable(X)` with `TypeVariable(X) <: TypeVariable(T)?`
    1. `TypeVariable(T) & Any <: TypeVariable(T)`
    2. `TypeVariable(T) <: TypeVariable(T)?`
7. Combine `TypeVariable(T) == @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(X) & Any` with `TypeVariable(T) <: ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) kotlin/String!`
    1. `TypeVariable(X) <: kotlin/String?`
8. Combine `kotlin/Nothing? <: TypeVariable(X)` with `TypeVariable(X) <: TypeVariable(T)?`
    1. `kotlin/Nothing <: TypeVariable(T)`
9. Combine `kotlin/String <: TypeVariable(T)` with `TypeVariable(T) == @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(X) & Any`
    1. `kotlin/String <: TypeVariable(X)`
10. Combine `TypeVariable(T) <: TypeVariable(X)` with `TypeVariable(X) <: kotlin/String?`
    1. `TypeVariable(T) <: kotlin/String?`
11. Combine `TypeVariable(X) <: kotlin/String?` with `TypeVariable(T) == @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(X) & Any`
    1. `TypeVariable(T) <: kotlin/String`
12. Combine `kotlin/Nothing <: TypeVariable(T)` with `TypeVariable(T) == @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(X) & Any`
    1. `kotlin/Nothing <: TypeVariable(X)`
13. Combine `kotlin/String <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/String`
    1. `TypeVariable(T) == kotlin/String`

##### Call Completion:

1. Choose `TypeVariable(X)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(T)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(X) == ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) kotlin/String!` _from Fix variable X_
3. Combine `TypeVariable(T) <: TypeVariable(X)` with `TypeVariable(X) == ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) kotlin/String!`
    1. `TypeVariable(T) <: ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) kotlin/String!`
4. Combine `TypeVariable(X) == ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) kotlin/String!` with `TypeVariable(X) <: TypeVariable(T)?`
    1. `kotlin/String <: TypeVariable(T)`
5. Combine `TypeVariable(X) == ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) kotlin/String!` with `TypeVariable(T) == @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(X) & Any`
    1. `TypeVariable(T) <: ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  kotlin/String`
    2. `ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  kotlin/String <: TypeVariable(T)`
6. Combine `TypeVariable(T) == @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(X) & Any` with `TypeVariable(T) <: ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  kotlin/String`
    1. `TypeVariable(X) <: @R|org/jetbrains/annotations/NotNull|()  kotlin/String?`
7. Combine `TypeVariable(T) <: ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  kotlin/String` with `ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  kotlin/String <: TypeVariable(T)`
    1. `TypeVariable(T) == ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  kotlin/String`
8. Combine `TypeVariable(T) == ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  kotlin/String` with `TypeVariable(T) == @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(X) & Any`
    1. `ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  kotlin/String <: TypeVariable(X)`
9. Combine `ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(coneType=kotlin/String?, relevantFeature=DontMakeExplicitNullableJavaTypeArgumentsFlexible) @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  kotlin/String <: TypeVariable(X)` with `TypeVariable(X) <: TypeVariable(T)?`
    1. `@R|org/jetbrains/annotations/NotNull|()  kotlin/String <: TypeVariable(T)`
10. Choose `TypeVariable(T)` with `Readiness(
    	 true ALLOWED
    	 true HAS_PROPER_CONSTRAINTS
    	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
    	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
    	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
    	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
    	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
    	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
    	false REIFIED
    	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
    	 true HAS_PROPER_NON_ILT_CONSTRAINT
    	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
    	 true HAS_PROPER_EQUALITY_CONSTRAINT
    	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
    )`
11. `TypeVariable(T) == kotlin/String` _from Fix variable T_
