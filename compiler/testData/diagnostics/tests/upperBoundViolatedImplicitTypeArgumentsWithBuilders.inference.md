## `Source session for module <main>`

### Call 1

```
@R|kotlin/Suppress|(String(UNCHECKED_CAST)) 
```

#### Candidate 1: `FirConstructorSymbol kotlin/Suppress.Suppress` --- `constructor(vararg names: String): Suppress`
##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String` _from Argument String(UNCHECKED_CAST)_

### Call 2

```
@R|kotlin/Suppress|(String(UNCHECKED_CAST), String(CAST_NEVER_SUCCEEDS)) 
```

#### Candidate 1: `FirConstructorSymbol kotlin/Suppress.Suppress` --- `constructor(vararg names: String): Suppress`
##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String` _from Argument String(UNCHECKED_CAST)_

### Call 3

```
(this@R|/removeTraitIfPresent|.R|SubstitutionOverride</ToSmithyBuilder.toBuilder: R|SmithyBuilder<T>|>|() as R|B|).R|SubstitutionOverride</AbstractShapeBuilder.removeTrait: R|B|>|().build#()
```

#### Candidate 1: `FirNamedFunctionSymbol /AbstractShapeBuilder.build` --- `fun build(): T↩`
##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `T <: T` _from ExpectedType for some call_

### Call 4

```
@R|kotlin/Suppress|(String(UNCHECKED_CAST)) 
```

#### Candidate 1: `FirConstructorSymbol kotlin/Suppress.Suppress` --- `constructor(vararg names: String): Suppress`
##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String` _from Argument String(UNCHECKED_CAST)_

### Call 5

```
R|<local>/shape|.removeTraitIfPresent#()
```

#### Candidate 1: `FirNamedFunctionSymbol /removeTraitIfPresent` --- `fun <T : Shape, ToSmithyBuilder<T>, B : AbstractShapeBuilder<B, T>, SmithyBuilder<T>> T.removeTraitIfPresent(): T`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /removeTraitIfPresent`s parameter 0
2. New `TypeVariable(B)` for `FirNamedFunctionSymbol /removeTraitIfPresent`s parameter 1
3. `TypeVariable(T) <: Shape` _from DeclaredUpperBound_
4. `TypeVariable(T) <: ToSmithyBuilder<TypeVariable(T)>` _from DeclaredUpperBound_
5. `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(T)>` _from DeclaredUpperBound_
6. `TypeVariable(B) <: SmithyBuilder<TypeVariable(T)>` _from DeclaredUpperBound_

##### Resolution Stages > CheckExtensionReceiver:

1. `OperationShape <: TypeVariable(T)` _from Receiver R|<local>/shape|_
2. Combine `OperationShape <: TypeVariable(T)` with `TypeVariable(T) <: ToSmithyBuilder<TypeVariable(T)>`
    1. `TypeVariable(T) <: OperationShape`
3. Combine `OperationShape <: TypeVariable(T)` with `TypeVariable(T) <: OperationShape`
    1. `TypeVariable(T) == OperationShape`
4. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(T) <: ToSmithyBuilder<TypeVariable(T)>`
    1. `TypeVariable(T) <: ToSmithyBuilder<OperationShape>`
5. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(B) <: SmithyBuilder<TypeVariable(T)>`
    1. `TypeVariable(B) <: SmithyBuilder<OperationShape>`

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(B)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(T) == OperationShape` _from Fix variable T_
3. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(T) <: ToSmithyBuilder<TypeVariable(T)>`
    1. `OperationShape <: TypeVariable(T)`
    2. `TypeVariable(T) <: OperationShape`
    3. `TypeVariable(T) <: ToSmithyBuilder<OperationShape>`
4. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(B) <: SmithyBuilder<TypeVariable(T)>`
    1. `TypeVariable(B) <: SmithyBuilder<OperationShape>`
5. Choose `TypeVariable(B)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
6. `TypeVariable(B) == SmithyBuilder<OperationShape>` _from Fix variable B_

### Call 6

```
R|<local>/shape|.removeTraitIfPresent#<R|OperationShape|, R|AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>|>()
```

#### Candidate 1: `FirNamedFunctionSymbol /removeTraitIfPresent` --- `fun <T : Shape, ToSmithyBuilder<T>, B : AbstractShapeBuilder<B, T>, SmithyBuilder<T>> T.removeTraitIfPresent(): T`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /removeTraitIfPresent`s parameter 0
2. New `TypeVariable(B)` for `FirNamedFunctionSymbol /removeTraitIfPresent`s parameter 1
3. `TypeVariable(T) <: Shape` _from DeclaredUpperBound_
4. `TypeVariable(T) <: ToSmithyBuilder<TypeVariable(T)>` _from DeclaredUpperBound_
5. `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(T)>` _from DeclaredUpperBound_
6. `TypeVariable(B) <: SmithyBuilder<TypeVariable(T)>` _from DeclaredUpperBound_
7. `TypeVariable(T) == OperationShape` _from TypeParameter R|OperationShape|_
8. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(T) <: ToSmithyBuilder<TypeVariable(T)>`
    1. `OperationShape <: TypeVariable(T)`
    2. `TypeVariable(T) <: OperationShape`
    3. `TypeVariable(T) <: ToSmithyBuilder<OperationShape>`
9. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(B) <: SmithyBuilder<TypeVariable(T)>`
    1. `TypeVariable(B) <: SmithyBuilder<OperationShape>`
10. `TypeVariable(B) == AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>` _from TypeParameter R|AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>|_
11. Combine `TypeVariable(B) == AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(T)>`
    1. `SmithyBuilder<OperationShape> <: TypeVariable(B)`
    2. `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>, TypeVariable(T)>`
12. Combine `TypeVariable(B) <: SmithyBuilder<OperationShape>` with `SmithyBuilder<OperationShape> <: TypeVariable(B)`
    1. `TypeVariable(B) == SmithyBuilder<OperationShape>`
13. __NewConstraintError: `SmithyBuilder<OperationShape> <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(T)>`__
14. __NewConstraintError: `SmithyBuilder<OperationShape> <: AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>`__
15. Combine `TypeVariable(B) == SmithyBuilder<OperationShape>` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(T)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<SmithyBuilder<OperationShape>, TypeVariable(T)>`
16. __NewConstraintError: `AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape> <: AbstractShapeBuilder<AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>, TypeVariable(T)>`__
17. __NewConstraintError: `SmithyBuilder<OperationShape> <: AbstractShapeBuilder<AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>, TypeVariable(T)>`__
18. __NewConstraintError: `SmithyBuilder<OperationShape> <: AbstractShapeBuilder<SmithyBuilder<OperationShape>, TypeVariable(T)>`__

##### Resolution Stages > CheckExtensionReceiver:

1. `OperationShape <: TypeVariable(T)` _from Receiver R|<local>/shape|_

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(B)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(T) == OperationShape` _from Fix variable T_
3. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(T) <: ToSmithyBuilder<TypeVariable(T)>`
    1. `OperationShape <: TypeVariable(T)`
    2. `TypeVariable(T) <: OperationShape`
    3. `TypeVariable(T) <: ToSmithyBuilder<OperationShape>`
4. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(B) <: SmithyBuilder<TypeVariable(T)>`
    1. `TypeVariable(B) <: SmithyBuilder<OperationShape>`
5. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>, TypeVariable(T)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>, OperationShape>`
6. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(B) <: AbstractShapeBuilder<SmithyBuilder<OperationShape>, TypeVariable(T)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>`
7. __NewConstraintError: `AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape> <: AbstractShapeBuilder<AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>, OperationShape>`__
8. __NewConstraintError: `SmithyBuilder<OperationShape> <: AbstractShapeBuilder<AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>, OperationShape>`__
9. Choose `TypeVariable(B)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
10. `TypeVariable(B) == AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>` _from Fix variable B_
11. __NewConstraintError: `SmithyBuilder<OperationShape> <: AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>`__
12. __NewConstraintError: `AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape> <: AbstractShapeBuilder<AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>, OperationShape>`__

### Call 7

```
R|<local>/shape|.removeTraitIfPresent#<R|OperationShape|, R|kotlin/Nothing|>()
```

#### Candidate 1: `FirNamedFunctionSymbol /removeTraitIfPresent` --- `fun <T : Shape, ToSmithyBuilder<T>, B : AbstractShapeBuilder<B, T>, SmithyBuilder<T>> T.removeTraitIfPresent(): T`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /removeTraitIfPresent`s parameter 0
2. New `TypeVariable(B)` for `FirNamedFunctionSymbol /removeTraitIfPresent`s parameter 1
3. `TypeVariable(T) <: Shape` _from DeclaredUpperBound_
4. `TypeVariable(T) <: ToSmithyBuilder<TypeVariable(T)>` _from DeclaredUpperBound_
5. `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(T)>` _from DeclaredUpperBound_
6. `TypeVariable(B) <: SmithyBuilder<TypeVariable(T)>` _from DeclaredUpperBound_
7. `TypeVariable(T) == OperationShape` _from TypeParameter R|OperationShape|_
8. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(T) <: ToSmithyBuilder<TypeVariable(T)>`
    1. `OperationShape <: TypeVariable(T)`
    2. `TypeVariable(T) <: OperationShape`
    3. `TypeVariable(T) <: ToSmithyBuilder<OperationShape>`
9. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(B) <: SmithyBuilder<TypeVariable(T)>`
    1. `TypeVariable(B) <: SmithyBuilder<OperationShape>`
10. `TypeVariable(B) == kotlin/Nothing` _from TypeParameter R|kotlin/Nothing|_
11. Combine `TypeVariable(B) == kotlin/Nothing` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(T)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<kotlin/Nothing, TypeVariable(T)>`

##### Resolution Stages > CheckExtensionReceiver:

1. `OperationShape <: TypeVariable(T)` _from Receiver R|<local>/shape|_

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(B)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(T) == OperationShape` _from Fix variable T_
3. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(T) <: ToSmithyBuilder<TypeVariable(T)>`
    1. `OperationShape <: TypeVariable(T)`
    2. `TypeVariable(T) <: OperationShape`
    3. `TypeVariable(T) <: ToSmithyBuilder<OperationShape>`
4. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(B) <: SmithyBuilder<TypeVariable(T)>`
    1. `TypeVariable(B) <: SmithyBuilder<OperationShape>`
5. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(B) <: AbstractShapeBuilder<kotlin/Nothing, TypeVariable(T)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<kotlin/Nothing, OperationShape>`
6. Choose `TypeVariable(B)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
7. `TypeVariable(B) == kotlin/Nothing` _from Fix variable B_

### Call 8

```
R|<local>/shape|.removeTraitIfPresent#<R|OperationShape|, R|AbstractShapeBuilder<*, OperationShape>|>()
```

#### Candidate 1: `FirNamedFunctionSymbol /removeTraitIfPresent` --- `fun <T : Shape, ToSmithyBuilder<T>, B : AbstractShapeBuilder<B, T>, SmithyBuilder<T>> T.removeTraitIfPresent(): T`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /removeTraitIfPresent`s parameter 0
2. New `TypeVariable(B)` for `FirNamedFunctionSymbol /removeTraitIfPresent`s parameter 1
3. `TypeVariable(T) <: Shape` _from DeclaredUpperBound_
4. `TypeVariable(T) <: ToSmithyBuilder<TypeVariable(T)>` _from DeclaredUpperBound_
5. `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(T)>` _from DeclaredUpperBound_
6. `TypeVariable(B) <: SmithyBuilder<TypeVariable(T)>` _from DeclaredUpperBound_
7. `TypeVariable(T) == OperationShape` _from TypeParameter R|OperationShape|_
8. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(T) <: ToSmithyBuilder<TypeVariable(T)>`
    1. `OperationShape <: TypeVariable(T)`
    2. `TypeVariable(T) <: OperationShape`
    3. `TypeVariable(T) <: ToSmithyBuilder<OperationShape>`
9. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(B) <: SmithyBuilder<TypeVariable(T)>`
    1. `TypeVariable(B) <: SmithyBuilder<OperationShape>`
10. `TypeVariable(B) == AbstractShapeBuilder<*, OperationShape>` _from TypeParameter R|AbstractShapeBuilder<*, OperationShape>|_
11. Combine `TypeVariable(B) == AbstractShapeBuilder<*, OperationShape>` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(T)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<*, OperationShape>, TypeVariable(T)>`
12. __NewConstraintError: `AbstractShapeBuilder<*, OperationShape> <: AbstractShapeBuilder<AbstractShapeBuilder<*, OperationShape>, TypeVariable(T)>`__

##### Resolution Stages > CheckExtensionReceiver:

1. `OperationShape <: TypeVariable(T)` _from Receiver R|<local>/shape|_

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(B)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(T) == OperationShape` _from Fix variable T_
3. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(T) <: ToSmithyBuilder<TypeVariable(T)>`
    1. `OperationShape <: TypeVariable(T)`
    2. `TypeVariable(T) <: OperationShape`
    3. `TypeVariable(T) <: ToSmithyBuilder<OperationShape>`
4. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(B) <: SmithyBuilder<TypeVariable(T)>`
    1. `TypeVariable(B) <: SmithyBuilder<OperationShape>`
5. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<*, OperationShape>, TypeVariable(T)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<*, OperationShape>, OperationShape>`
6. __NewConstraintError: `AbstractShapeBuilder<*, OperationShape> <: AbstractShapeBuilder<AbstractShapeBuilder<*, OperationShape>, OperationShape>`__
7. Choose `TypeVariable(B)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
8. `TypeVariable(B) == AbstractShapeBuilder<*, OperationShape>` _from Fix variable B_
9. __NewConstraintError: `AbstractShapeBuilder<*, OperationShape> <: AbstractShapeBuilder<AbstractShapeBuilder<*, OperationShape>, OperationShape>`__

### Call 9

```
shapeToBuilder#(R|<local>/target|)
```

#### Candidate 1: `FirNamedFunctionSymbol /shapeToBuilder` --- `@Suppress(names = vararg(String(UNCHECKED_CAST), String(CAST_NEVER_SUCCEEDS)) [evaluated = vararg(String(UNCHECKED_CAST), String(CAST_NEVER_SUCCEEDS))]) fun <B : AbstractShapeBuilder<B, S>, S : Shape> shapeToBuilder(shape: S): B`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(B)` for `FirNamedFunctionSymbol /shapeToBuilder`s parameter 0
2. New `TypeVariable(S)` for `FirNamedFunctionSymbol /shapeToBuilder`s parameter 1
3. `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(S)>` _from DeclaredUpperBound_
4. `TypeVariable(S) <: Shape` _from DeclaredUpperBound_

##### Resolution Stages > CheckArguments:

1. `SimpleShape <: TypeVariable(S)` _from Argument R|<local>/target|_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `TypeVariable(B) <: AbstractShapeBuilder<*, *>` _from ExpectedType for some call_
2. Combine `TypeVariable(B) <: AbstractShapeBuilder<*, *>` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, TypeVariable(S)>`

##### Call Completion:

1. Choose `TypeVariable(S)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(B)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(S) == SimpleShape` _from Fix variable S_
3. Combine `TypeVariable(S) == SimpleShape` with `TypeVariable(B) <: AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>`
4. Choose `TypeVariable(B)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
5. `TypeVariable(B) == AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>` _from Fix variable B_

### Call 10

```
shapeToBuilder#<R|AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>|, R|SimpleShape|>(R|<local>/target|)
```

#### Candidate 1: `FirNamedFunctionSymbol /shapeToBuilder` --- `@Suppress(names = vararg(String(UNCHECKED_CAST), String(CAST_NEVER_SUCCEEDS)) [evaluated = vararg(String(UNCHECKED_CAST), String(CAST_NEVER_SUCCEEDS))]) fun <B : AbstractShapeBuilder<B, S>, S : Shape> shapeToBuilder(shape: S): B`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(B)` for `FirNamedFunctionSymbol /shapeToBuilder`s parameter 0
2. New `TypeVariable(S)` for `FirNamedFunctionSymbol /shapeToBuilder`s parameter 1
3. `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(S)>` _from DeclaredUpperBound_
4. `TypeVariable(S) <: Shape` _from DeclaredUpperBound_
5. `TypeVariable(B) == AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>` _from TypeParameter R|AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>|_
6. Combine `TypeVariable(B) == AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(S)>`
    1. `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: TypeVariable(B)`
    2. `TypeVariable(B) <: AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>`
    3. `SimpleShape <: TypeVariable(S)`
    4. `TypeVariable(S) <: SimpleShape`
    5. `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, TypeVariable(S)>`
7. Combine `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: TypeVariable(B)` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(S)>`
    1. `AbstractShapeBuilder<*, *> <: TypeVariable(B)`
    2. `TypeVariable(B) <: kotlin/Nothing`
8. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>`__
9. Combine `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: TypeVariable(B)` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, TypeVariable(S)>`
10. Combine `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: TypeVariable(B)` with `TypeVariable(B) <: AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>`
    1. `TypeVariable(B) == AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>`
11. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>`__
12. Combine `TypeVariable(B) == AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, TypeVariable(S)>`
13. Combine `SimpleShape <: TypeVariable(S)` with `TypeVariable(S) <: SimpleShape`
    1. `TypeVariable(S) == SimpleShape`
14. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, TypeVariable(S)>`__
15. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, TypeVariable(S)>`__
16. Combine `AbstractShapeBuilder<*, *> <: TypeVariable(B)` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(S)>`
    1. `Shape <: TypeVariable(S)`
    2. `TypeVariable(S) <: kotlin/Nothing`
17. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>`__
18. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>`__
19. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, TypeVariable(S)>`__
20. Combine `AbstractShapeBuilder<*, *> <: TypeVariable(B)` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<in AbstractShapeBuilder<*, *>, TypeVariable(S)>`
21. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: kotlin/Nothing`__
22. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: kotlin/Nothing`__
23. __NewConstraintError: `AbstractShapeBuilder<*, *> <: kotlin/Nothing`__
24. Combine `TypeVariable(B) <: kotlin/Nothing` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<kotlin/Nothing, TypeVariable(S)>`
25. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, TypeVariable(S)>`__
26. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, TypeVariable(S)>`__
27. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, TypeVariable(S)>`__
28. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, TypeVariable(S)>`__
29. Combine `TypeVariable(S) <: Shape` with `Shape <: TypeVariable(S)`
    1. `TypeVariable(S) == Shape`
30. __NewConstraintError: `Shape <: SimpleShape`__
31. Combine `TypeVariable(S) == Shape` with `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, Shape>`
32. Combine `TypeVariable(S) == Shape` with `TypeVariable(B) <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, Shape>`
33. Combine `TypeVariable(S) == Shape` with `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, Shape>`
34. __NewConstraintError: `SimpleShape <: kotlin/Nothing`__
35. __NewConstraintError: `Shape <: kotlin/Nothing`__
36. Combine `TypeVariable(S) <: kotlin/Nothing` with `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, kotlin/Nothing>`
37. Combine `TypeVariable(S) <: kotlin/Nothing` with `TypeVariable(B) <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, kotlin/Nothing>`
38. Combine `TypeVariable(S) <: kotlin/Nothing` with `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, kotlin/Nothing>`
39. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<in AbstractShapeBuilder<*, *>, TypeVariable(S)>`__
40. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<in AbstractShapeBuilder<*, *>, TypeVariable(S)>`__
41. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<in AbstractShapeBuilder<*, *>, TypeVariable(S)>`__
42. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<kotlin/Nothing, TypeVariable(S)>`__
43. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<kotlin/Nothing, TypeVariable(S)>`__
44. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<kotlin/Nothing, TypeVariable(S)>`__
45. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, Shape>`__
46. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, Shape>`__
47. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, Shape>`__
48. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, Shape>`__
49. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, Shape>`__
50. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, Shape>`__
51. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, Shape>`__
52. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, Shape>`__
53. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, Shape>`__
54. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, kotlin/Nothing>`__
55. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, kotlin/Nothing>`__
56. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, kotlin/Nothing>`__
57. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, kotlin/Nothing>`__
58. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, kotlin/Nothing>`__
59. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, kotlin/Nothing>`__
60. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, kotlin/Nothing>`__
61. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, kotlin/Nothing>`__
62. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, kotlin/Nothing>`__

##### Call Completion:

1. Choose `TypeVariable(S)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(B)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(S) == SimpleShape` _from Fix variable S_
3. __NewConstraintError: `Shape <: SimpleShape`__
4. __NewConstraintError: `SimpleShape <: kotlin/Nothing`__
5. Combine `TypeVariable(S) == SimpleShape` with `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, SimpleShape>`
6. Combine `TypeVariable(S) == SimpleShape` with `TypeVariable(B) <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>`
7. Combine `TypeVariable(S) == SimpleShape` with `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>`
8. Combine `TypeVariable(S) == SimpleShape` with `TypeVariable(B) <: AbstractShapeBuilder<in AbstractShapeBuilder<*, *>, TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<in AbstractShapeBuilder<*, *>, SimpleShape>`
9. Combine `TypeVariable(S) == SimpleShape` with `TypeVariable(B) <: AbstractShapeBuilder<kotlin/Nothing, TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<kotlin/Nothing, SimpleShape>`
10. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, SimpleShape>`__
11. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, SimpleShape>`__
12. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, SimpleShape>`__
13. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>`__
14. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>`__
15. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<in AbstractShapeBuilder<*, *>, SimpleShape>`__
16. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<in AbstractShapeBuilder<*, *>, SimpleShape>`__
17. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<in AbstractShapeBuilder<*, *>, SimpleShape>`__
18. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<kotlin/Nothing, SimpleShape>`__
19. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<kotlin/Nothing, SimpleShape>`__
20. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<kotlin/Nothing, SimpleShape>`__
21. Choose `TypeVariable(B)` with `Readiness(
    	 true ALLOWED
    	 true HAS_PROPER_CONSTRAINTS
    	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
    	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
    	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
    	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
    	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
    	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
    	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
    	 true HAS_PROPER_NON_ILT_CONSTRAINT
    	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
    	 true HAS_PROPER_EQUALITY_CONSTRAINT
    	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
    )`
22. `TypeVariable(B) == AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>` _from Fix variable B_
23. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>`__
24. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>`__
25. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: kotlin/Nothing`__
26. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, Shape>`__
27. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, Shape>`__
28. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, Shape>`__
29. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, kotlin/Nothing>`__
30. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<in AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, kotlin/Nothing>`__
31. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, kotlin/Nothing>`__
32. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>, SimpleShape>`__
33. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<in AbstractShapeBuilder<*, *>, SimpleShape>`__
34. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: AbstractShapeBuilder<kotlin/Nothing, SimpleShape>`__

### Call 11

```
shapeToBuilder#<R|AbstractShapeBuilder<*, SimpleShape>|, R|SimpleShape|>(R|<local>/target|)
```

#### Candidate 1: `FirNamedFunctionSymbol /shapeToBuilder` --- `@Suppress(names = vararg(String(UNCHECKED_CAST), String(CAST_NEVER_SUCCEEDS)) [evaluated = vararg(String(UNCHECKED_CAST), String(CAST_NEVER_SUCCEEDS))]) fun <B : AbstractShapeBuilder<B, S>, S : Shape> shapeToBuilder(shape: S): B`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(B)` for `FirNamedFunctionSymbol /shapeToBuilder`s parameter 0
2. New `TypeVariable(S)` for `FirNamedFunctionSymbol /shapeToBuilder`s parameter 1
3. `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(S)>` _from DeclaredUpperBound_
4. `TypeVariable(S) <: Shape` _from DeclaredUpperBound_
5. `TypeVariable(B) == AbstractShapeBuilder<*, SimpleShape>` _from TypeParameter R|AbstractShapeBuilder<*, SimpleShape>|_
6. Combine `TypeVariable(B) == AbstractShapeBuilder<*, SimpleShape>` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(S)>`
    1. `SimpleShape <: TypeVariable(S)`
    2. `TypeVariable(S) <: SimpleShape`
    3. `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<*, SimpleShape>, TypeVariable(S)>`
7. Combine `SimpleShape <: TypeVariable(S)` with `TypeVariable(S) <: SimpleShape`
    1. `TypeVariable(S) == SimpleShape`
8. __NewConstraintError: `AbstractShapeBuilder<*, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<*, SimpleShape>, TypeVariable(S)>`__

##### Call Completion:

1. Choose `TypeVariable(S)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(B)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(S) == SimpleShape` _from Fix variable S_
3. Combine `TypeVariable(S) == SimpleShape` with `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<*, SimpleShape>, TypeVariable(S)>`
    1. `TypeVariable(B) <: AbstractShapeBuilder<AbstractShapeBuilder<*, SimpleShape>, SimpleShape>`
4. __NewConstraintError: `AbstractShapeBuilder<*, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<*, SimpleShape>, SimpleShape>`__
5. Choose `TypeVariable(B)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
6. `TypeVariable(B) == AbstractShapeBuilder<*, SimpleShape>` _from Fix variable B_
7. __NewConstraintError: `AbstractShapeBuilder<*, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<*, SimpleShape>, SimpleShape>`__
