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

#### Candidate 1: `FirNamedFunctionSymbol /AbstractShapeBuilder.build` --- `fun build(): T`
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

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(B)` is `Readiness(
       	 true ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(T) == OperationShape` _from Fix variable T_
3. Combine `TypeVariable(T) == OperationShape` with `TypeVariable(T) <: ToSmithyBuilder<TypeVariable(T)>`
    1. `OperationShape <: TypeVariable(T)`
    2. `TypeVariable(T) <: OperationShape`
4. `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), OperationShape>` _from Fix variable T_
5. `TypeVariable(B) <: SmithyBuilder<OperationShape>` _from Fix variable T_
6. Choose `TypeVariable(B)` with `Readiness(
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
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
7. `TypeVariable(B) == CapturedType(*)` _from Fix variable B_
8. Combine `TypeVariable(B) == CapturedType(*)` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), OperationShape>`
    1. `CapturedType(*) <: TypeVariable(B)`
    2. `TypeVariable(B) <: CapturedType(*)`

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
9. `TypeVariable(B) == AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>` _from TypeParameter R|AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>|_
10. Combine `TypeVariable(B) == AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(T)>`
    1. `SmithyBuilder<OperationShape> <: TypeVariable(B)`
    2. `TypeVariable(B) <: SmithyBuilder<OperationShape>`
11. __NewConstraintError: `SmithyBuilder<OperationShape> <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(T)>`__
12. __NewConstraintError: `SmithyBuilder<OperationShape> <: AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>`__
13. Combine `SmithyBuilder<OperationShape> <: TypeVariable(B)` with `TypeVariable(B) <: SmithyBuilder<OperationShape>`
    1. `TypeVariable(B) == SmithyBuilder<OperationShape>`
14. __NewConstraintError: `SmithyBuilder<OperationShape> <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(T)>`__
15. __NewConstraintError: `SmithyBuilder<OperationShape> <: AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>`__

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
   	false REIFIED
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
       	false REIFIED
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
4. `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), OperationShape>` _from Fix variable T_
5. `TypeVariable(B) <: SmithyBuilder<OperationShape>` _from Fix variable T_
6. Choose `TypeVariable(B)` with `Readiness(
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
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
7. `TypeVariable(B) == AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>` _from Fix variable B_
8. __NewConstraintError: `SmithyBuilder<OperationShape> <: AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>`__
9. Combine `TypeVariable(B) == AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), OperationShape>`
    1. `SmithyBuilder<OperationShape> <: TypeVariable(B)`

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
9. `TypeVariable(B) == kotlin/Nothing` _from TypeParameter R|kotlin/Nothing|_

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
   	false REIFIED
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
       	false REIFIED
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
4. `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), OperationShape>` _from Fix variable T_
5. `TypeVariable(B) <: SmithyBuilder<OperationShape>` _from Fix variable T_
6. Choose `TypeVariable(B)` with `Readiness(
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
9. `TypeVariable(B) == AbstractShapeBuilder<*, OperationShape>` _from TypeParameter R|AbstractShapeBuilder<*, OperationShape>|_
10. Combine `TypeVariable(B) == AbstractShapeBuilder<*, OperationShape>` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(T)>`
    1. `AbstractShapeBuilder<*, OperationShape> <: TypeVariable(B)`
    2. `TypeVariable(B) <: kotlin/Nothing`
11. __NewConstraintError: `AbstractShapeBuilder<*, OperationShape> <: kotlin/Nothing`__

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
   	false REIFIED
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
       	false REIFIED
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
4. `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), OperationShape>` _from Fix variable T_
5. `TypeVariable(B) <: SmithyBuilder<OperationShape>` _from Fix variable T_
6. Choose `TypeVariable(B)` with `Readiness(
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
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
7. `TypeVariable(B) == AbstractShapeBuilder<*, OperationShape>` _from Fix variable B_
8. __NewConstraintError: `AbstractShapeBuilder<*, OperationShape> <: kotlin/Nothing`__
9. Combine `TypeVariable(B) == AbstractShapeBuilder<*, OperationShape>` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), OperationShape>`
    1. `AbstractShapeBuilder<*, OperationShape> <: TypeVariable(B)`
    2. `TypeVariable(B) <: kotlin/Nothing`

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
   	false REIFIED
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
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(S) == SimpleShape` _from Fix variable S_
3. `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), SimpleShape>` _from Fix variable S_
4. Choose `TypeVariable(B)` with `Readiness(
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
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
5. `TypeVariable(B) == CapturedType(*)` _from Fix variable B_
6. Combine `TypeVariable(B) == CapturedType(*)` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), SimpleShape>`
    1. `CapturedType(*) <: TypeVariable(B)`
    2. `TypeVariable(B) <: CapturedType(*)`

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
7. Combine `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: TypeVariable(B)` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(S)>`
    1. `AbstractShapeBuilder<*, *> <: TypeVariable(B)`
    2. `TypeVariable(B) <: kotlin/Nothing`
8. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>`__
9. Combine `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: TypeVariable(B)` with `TypeVariable(B) <: AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>`
    1. `TypeVariable(B) == AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>`
10. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>`__
11. Combine `SimpleShape <: TypeVariable(S)` with `TypeVariable(S) <: SimpleShape`
    1. `TypeVariable(S) == SimpleShape`
12. Combine `AbstractShapeBuilder<*, *> <: TypeVariable(B)` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), TypeVariable(S)>`
    1. `Shape <: TypeVariable(S)`
    2. `TypeVariable(S) <: kotlin/Nothing`
13. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>`__
14. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>`__
15. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: kotlin/Nothing`__
16. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: kotlin/Nothing`__
17. __NewConstraintError: `AbstractShapeBuilder<*, *> <: kotlin/Nothing`__
18. Combine `TypeVariable(S) <: Shape` with `Shape <: TypeVariable(S)`
    1. `TypeVariable(S) == Shape`
19. __NewConstraintError: `Shape <: SimpleShape`__
20. __NewConstraintError: `SimpleShape <: kotlin/Nothing`__
21. __NewConstraintError: `Shape <: kotlin/Nothing`__

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
   	false REIFIED
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
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(S) == SimpleShape` _from Fix variable S_
3. __NewConstraintError: `Shape <: SimpleShape`__
4. __NewConstraintError: `SimpleShape <: kotlin/Nothing`__
5. `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), SimpleShape>` _from Fix variable S_
6. Choose `TypeVariable(B)` with `Readiness(
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
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
7. `TypeVariable(B) == AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>` _from Fix variable B_
8. __NewConstraintError: `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>`__
9. __NewConstraintError: `AbstractShapeBuilder<*, *> <: AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>`__
10. __NewConstraintError: `AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape> <: kotlin/Nothing`__
11. Combine `TypeVariable(B) == AbstractShapeBuilder<AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>, SimpleShape>` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), SimpleShape>`
    1. `AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape> <: TypeVariable(B)`
    2. `TypeVariable(B) <: AbstractShapeBuilder<out AbstractShapeBuilder<*, *>, SimpleShape>`

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
    1. `AbstractShapeBuilder<*, SimpleShape> <: TypeVariable(B)`
    2. `TypeVariable(B) <: kotlin/Nothing`
    3. `SimpleShape <: TypeVariable(S)`
    4. `TypeVariable(S) <: SimpleShape`
7. __NewConstraintError: `AbstractShapeBuilder<*, SimpleShape> <: kotlin/Nothing`__
8. Combine `SimpleShape <: TypeVariable(S)` with `TypeVariable(S) <: SimpleShape`
    1. `TypeVariable(S) == SimpleShape`

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
   	false REIFIED
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
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(S) == SimpleShape` _from Fix variable S_
3. `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), SimpleShape>` _from Fix variable S_
4. Choose `TypeVariable(B)` with `Readiness(
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
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
5. `TypeVariable(B) == AbstractShapeBuilder<*, SimpleShape>` _from Fix variable B_
6. __NewConstraintError: `AbstractShapeBuilder<*, SimpleShape> <: kotlin/Nothing`__
7. Combine `TypeVariable(B) == AbstractShapeBuilder<*, SimpleShape>` with `TypeVariable(B) <: AbstractShapeBuilder<TypeVariable(B), SimpleShape>`
    1. `AbstractShapeBuilder<*, SimpleShape> <: TypeVariable(B)`
    2. `TypeVariable(B) <: kotlin/Nothing`
