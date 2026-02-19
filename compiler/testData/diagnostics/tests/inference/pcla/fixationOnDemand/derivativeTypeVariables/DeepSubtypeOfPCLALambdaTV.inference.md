## `Source session for module <main>`

### Call 1

```
pcla#(<L> = pcla@fun <implicit>.<anonymous>(otvOwner: <implicit>): <implicit> <inline=Unknown>  {
    lval pntvOwner: <implicit> = otvOwner#.createDerivativeTypeVariable#(otvOwner#.provideContainer#())
    pntvOwner#.constrain#(ScopeOwner#())
    otvOwner#.provide#().function#()
    otvOwner#.constrain#(Interloper#)
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /pcla` --- `fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(OT)` for `FirNamedFunctionSymbol /pcla`s parameter 0

##### Resolution Stages > CheckArguments:

1. `(TypeVariableOwner<TypeVariable(OT)>) -> kotlin/Unit <: (TypeVariableOwner<TypeVariable(OT)>) -> kotlin/Unit` _from Argument <L> = pcla <implicit>.<anonymous>(otvOwner: <implicit>): <implicit> <inline=Unknown>  {↩    lval pntvOwner: <implicit> = otvOwner#.createDerivativeTypeVariable#(otvOwner#.provideContainer#())↩    pntvOwner#.constrain#(ScopeOwner#())↩    otvOwner#.provide#().function#()↩    otvOwner#.constrain#(Interloper#)↩}↩_

##### Call Completion:

1. Choose `TypeVariable(OT)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. Choose `TypeVariable(OT)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 2

```
R?C|<local>/otvOwner|.createDerivativeTypeVariable#(R?C|<local>/otvOwner|.R?C|/TypeVariableOwner.provideContainer|())
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable` --- `fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: ContravariantContainer<PNT>): TypeVariableOwner<PNT>↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(PNT)` for `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable`s parameter 0

##### Resolution Stages > CheckArguments:

1. `ContravariantContainer<TypeVariable(OT)> <: ContravariantContainer<TypeVariable(PNT)>` _from Argument R?C|<local>/otvOwner|.R?C|/TypeVariableOwner.provideContainer|()_
    1. `TypeVariable(PNT) <: TypeVariable(OT)`

#### Candidate 2: `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable` --- `fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: InvariantContainer<in PNT>): TypeVariableOwner<PNT>↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(PNT)` for `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable`s parameter 0

##### Resolution Stages > CheckArguments:

1. `ContravariantContainer<TypeVariable(OT)> <: InvariantContainer<in TypeVariable(PNT)>` _from Argument R?C|<local>/otvOwner|.R?C|/TypeVariableOwner.provideContainer|()_
2. __NewConstraintError: `ContravariantContainer<TypeVariable(OT)> <: InvariantContainer<in TypeVariable(PNT)>`__

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable` --- `fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: ContravariantContainer<PNT>): TypeVariableOwner<PNT>↩`
##### Call Completion:

1. Choose `TypeVariable(PNT)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 3

```
R?C|<local>/pntvOwner|.constrain#(R|/ScopeOwner.ScopeOwner|())
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.constrain` --- `fun constrain(subtypeValue: ScopeOwner): Unit↩`
##### Resolution Stages > CheckArguments:

1. `ScopeOwner <: TypeVariable(PNT)` _from Argument R|/ScopeOwner.ScopeOwner|()_
2. Combine `ScopeOwner <: TypeVariable(PNT)` with `TypeVariable(PNT) <: TypeVariable(OT)`
    1. `ScopeOwner <: TypeVariable(OT)`

### Call 4

```
R?C|<local>/otvOwner|.provide#()
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.provide` --- `fun provide(): ScopeOwner↩`
##### Call Completion:

1. `TypeVariable(OT) == ScopeOwner` _from Fix variable OT_
2. Combine `TypeVariable(PNT) <: TypeVariable(OT)` with `TypeVariable(OT) == ScopeOwner`
    1. `TypeVariable(PNT) <: ScopeOwner`
3. Combine `ScopeOwner <: TypeVariable(PNT)` with `TypeVariable(PNT) <: ScopeOwner`
    1. `TypeVariable(PNT) == ScopeOwner`
4. Combine `TypeVariable(PNT) == ScopeOwner` with `TypeVariable(PNT) <: TypeVariable(OT)`
    1. `ScopeOwner <: TypeVariable(OT)`

### Call 5

```
R?C|<local>/otvOwner|.constrain#(Q|Interloper|)
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.constrain` --- `fun constrain(subtypeValue: ScopeOwner): Unit↩`
##### Resolution Stages > CheckArguments:

1. `Interloper <: TypeVariable(OT)` _from Argument Q|Interloper|_
2. __NewConstraintError: `Interloper <: ScopeOwner`__

### Call 1

```
pcla#(<L> = pcla@fun <implicit>.<anonymous>(otvOwner: <implicit>): <implicit> <inline=Unknown>  {
    lval pntvOwner: <implicit> = otvOwner#.createDerivativeTypeVariable#(otvOwner#.provideContainer#())
    pntvOwner#.constrain#(ScopeOwner#())
    otvOwner#.provide#().function#()
    otvOwner#.constrain#(Interloper#)
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /pcla` --- `fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(OT)` with `Readiness(
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
    1. `TypeVariable(PNT)` is `Readiness(
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
3. `TypeVariable(OT) == ScopeOwner` _from Fix variable OT_
4. Combine `TypeVariable(PNT) <: TypeVariable(OT)` with `TypeVariable(OT) == ScopeOwner`
    1. `TypeVariable(PNT) <: ScopeOwner`
5. Choose `TypeVariable(PNT)` with `Readiness(
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
6. `TypeVariable(PNT) == ScopeOwner` _from Fix variable PNT_

### Call 6

```
pcla#(<L> = pcla@fun <implicit>.<anonymous>(otvOwner: <implicit>): <implicit> <inline=Unknown>  {
    lval pntvOwner: <implicit> = otvOwner#.createDerivativeTypeVariable#(otvOwner#.provideContainer#())
    otvOwner#.constrain#(ScopeOwner#())
    pntvOwner#.provide#().function#()
    otvOwner#.constrain#(Interloper#)
    pntvOwner#.constrain#(Interloper#)
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /pcla` --- `fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(OT)` for `FirNamedFunctionSymbol /pcla`s parameter 0

##### Resolution Stages > CheckArguments:

1. `(TypeVariableOwner<TypeVariable(OT)>) -> kotlin/Unit <: (TypeVariableOwner<TypeVariable(OT)>) -> kotlin/Unit` _from Argument <L> = pcla <implicit>.<anonymous>(otvOwner: <implicit>): <implicit> <inline=Unknown>  {↩    lval pntvOwner: <implicit> = otvOwner#.createDerivativeTypeVariable#(otvOwner#.provideContainer#())↩    otvOwner#.constrain#(ScopeOwner#())↩    pntvOwner#.provide#().function#()↩    otvOwner#.constrain#(Interloper#)↩    pntvOwner#.constrain#(Interloper#)↩}↩_

##### Call Completion:

1. Choose `TypeVariable(OT)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. Choose `TypeVariable(OT)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 7

```
R?C|<local>/otvOwner|.createDerivativeTypeVariable#(R?C|<local>/otvOwner|.R?C|/TypeVariableOwner.provideContainer|())
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable` --- `fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: ContravariantContainer<PNT>): TypeVariableOwner<PNT>↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(PNT)` for `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable`s parameter 0

##### Resolution Stages > CheckArguments:

1. `ContravariantContainer<TypeVariable(OT)> <: ContravariantContainer<TypeVariable(PNT)>` _from Argument R?C|<local>/otvOwner|.R?C|/TypeVariableOwner.provideContainer|()_
    1. `TypeVariable(PNT) <: TypeVariable(OT)`

#### Candidate 2: `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable` --- `fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: InvariantContainer<in PNT>): TypeVariableOwner<PNT>↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(PNT)` for `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable`s parameter 0

##### Resolution Stages > CheckArguments:

1. `ContravariantContainer<TypeVariable(OT)> <: InvariantContainer<in TypeVariable(PNT)>` _from Argument R?C|<local>/otvOwner|.R?C|/TypeVariableOwner.provideContainer|()_
2. __NewConstraintError: `ContravariantContainer<TypeVariable(OT)> <: InvariantContainer<in TypeVariable(PNT)>`__

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable` --- `fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: ContravariantContainer<PNT>): TypeVariableOwner<PNT>↩`
##### Call Completion:

1. Choose `TypeVariable(PNT)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 8

```
R?C|<local>/otvOwner|.constrain#(R|/ScopeOwner.ScopeOwner|())
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.constrain` --- `fun constrain(subtypeValue: BaseType): Unit↩`
##### Resolution Stages > CheckArguments:

1. `ScopeOwner <: TypeVariable(OT)` _from Argument R|/ScopeOwner.ScopeOwner|()_

### Call 9

```
R?C|<local>/otvOwner|.constrain#(Q|Interloper|)
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.constrain` --- `fun constrain(subtypeValue: BaseType): Unit↩`
##### Resolution Stages > CheckArguments:

1. `Interloper <: TypeVariable(OT)` _from Argument Q|Interloper|_

### Call 10

```
R?C|<local>/pntvOwner|.constrain#(Q|Interloper|)
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.constrain` --- `fun constrain(subtypeValue: Interloper): Unit↩`
##### Resolution Stages > CheckArguments:

1. `Interloper <: TypeVariable(PNT)` _from Argument Q|Interloper|_
2. Combine `Interloper <: TypeVariable(PNT)` with `TypeVariable(PNT) <: TypeVariable(OT)`
    1. `Interloper <: TypeVariable(OT)`

### Call 6

```
pcla#(<L> = pcla@fun <implicit>.<anonymous>(otvOwner: <implicit>): <implicit> <inline=Unknown>  {
    lval pntvOwner: <implicit> = otvOwner#.createDerivativeTypeVariable#(otvOwner#.provideContainer#())
    otvOwner#.constrain#(ScopeOwner#())
    pntvOwner#.provide#().function#()
    otvOwner#.constrain#(Interloper#)
    pntvOwner#.constrain#(Interloper#)
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /pcla` --- `fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(OT)` with `Readiness(
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
    1. `TypeVariable(PNT)` is `Readiness(
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
3. `TypeVariable(OT) == BaseType` _from Fix variable OT_
4. Combine `TypeVariable(PNT) <: TypeVariable(OT)` with `TypeVariable(OT) == BaseType`
    1. `TypeVariable(PNT) <: BaseType`
5. Choose `TypeVariable(PNT)` with `Readiness(
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
6. `TypeVariable(PNT) == Interloper` _from Fix variable PNT_

### Call 11

```
pcla#(<L> = pcla@fun <implicit>.<anonymous>(otvOwner: <implicit>): <implicit> <inline=Unknown>  {
    lval pntvOwner: <implicit> = otvOwner#.createDerivativeTypeVariable#(otvOwner#.provideProjectedContainer#())
    pntvOwner#.constrain#(ScopeOwner#())
    otvOwner#.provide#().function#()
    otvOwner#.constrain#(Interloper#)
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /pcla` --- `fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(OT)` for `FirNamedFunctionSymbol /pcla`s parameter 0

##### Resolution Stages > CheckArguments:

1. `(TypeVariableOwner<TypeVariable(OT)>) -> kotlin/Unit <: (TypeVariableOwner<TypeVariable(OT)>) -> kotlin/Unit` _from Argument <L> = pcla <implicit>.<anonymous>(otvOwner: <implicit>): <implicit> <inline=Unknown>  {↩    lval pntvOwner: <implicit> = otvOwner#.createDerivativeTypeVariable#(otvOwner#.provideProjectedContainer#())↩    pntvOwner#.constrain#(ScopeOwner#())↩    otvOwner#.provide#().function#()↩    otvOwner#.constrain#(Interloper#)↩}↩_

##### Call Completion:

1. Choose `TypeVariable(OT)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. Choose `TypeVariable(OT)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 12

```
R?C|<local>/otvOwner|.createDerivativeTypeVariable#(R?C|<local>/otvOwner|.R?C|/TypeVariableOwner.provideProjectedContainer|())
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable` --- `fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: ContravariantContainer<PNT>): TypeVariableOwner<PNT>↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(PNT)` for `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable`s parameter 0

##### Resolution Stages > CheckArguments:

1. `InvariantContainer<CapturedType(in TypeVariable(OT))> <: ContravariantContainer<TypeVariable(PNT)>` _from Argument R?C|<local>/otvOwner|.R?C|/TypeVariableOwner.provideProjectedContainer|()_
2. __NewConstraintError: `InvariantContainer<CapturedType(in TypeVariable(OT))> <: ContravariantContainer<TypeVariable(PNT)>`__

#### Candidate 2: `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable` --- `fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: InvariantContainer<in PNT>): TypeVariableOwner<PNT>↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(PNT)` for `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable`s parameter 0

##### Resolution Stages > CheckArguments:

1. `InvariantContainer<CapturedType(in TypeVariable(OT))> <: InvariantContainer<in TypeVariable(PNT)>` _from Argument R?C|<local>/otvOwner|.R?C|/TypeVariableOwner.provideProjectedContainer|()_
    1. `TypeVariable(PNT) <: TypeVariable(OT)`
    2. `TypeVariable(PNT) <: CapturedType(in TypeVariable(OT))`

##### Call Completion:

1. Choose `TypeVariable(PNT)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 13

```
R?C|<local>/pntvOwner|.constrain#(R|/ScopeOwner.ScopeOwner|())
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.constrain` --- `fun constrain(subtypeValue: ScopeOwner): Unit↩`
##### Resolution Stages > CheckArguments:

1. `ScopeOwner <: TypeVariable(PNT)` _from Argument R|/ScopeOwner.ScopeOwner|()_
2. Combine `ScopeOwner <: TypeVariable(PNT)` with `TypeVariable(PNT) <: TypeVariable(OT)`
    1. `ScopeOwner <: TypeVariable(OT)`

### Call 14

```
R?C|<local>/otvOwner|.provide#()
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.provide` --- `fun provide(): ScopeOwner↩`
##### Call Completion:

1. `TypeVariable(OT) == ScopeOwner` _from Fix variable OT_
2. Combine `TypeVariable(PNT) <: TypeVariable(OT)` with `TypeVariable(OT) == ScopeOwner`
    1. `TypeVariable(PNT) <: ScopeOwner`
3. Combine `TypeVariable(OT) == ScopeOwner` with `TypeVariable(PNT) <: CapturedType(in TypeVariable(OT))`
    1. `TypeVariable(PNT) <: CapturedType(in ScopeOwner)`
4. Combine `ScopeOwner <: TypeVariable(PNT)` with `TypeVariable(PNT) <: ScopeOwner`
    1. `TypeVariable(PNT) == ScopeOwner`
5. Combine `TypeVariable(PNT) == ScopeOwner` with `TypeVariable(PNT) <: TypeVariable(OT)`
    1. `ScopeOwner <: TypeVariable(OT)`

### Call 15

```
R?C|<local>/otvOwner|.constrain#(Q|Interloper|)
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.constrain` --- `fun constrain(subtypeValue: ScopeOwner): Unit↩`
##### Resolution Stages > CheckArguments:

1. `Interloper <: TypeVariable(OT)` _from Argument Q|Interloper|_
2. __NewConstraintError: `Interloper <: ScopeOwner`__

### Call 11

```
pcla#(<L> = pcla@fun <implicit>.<anonymous>(otvOwner: <implicit>): <implicit> <inline=Unknown>  {
    lval pntvOwner: <implicit> = otvOwner#.createDerivativeTypeVariable#(otvOwner#.provideProjectedContainer#())
    pntvOwner#.constrain#(ScopeOwner#())
    otvOwner#.provide#().function#()
    otvOwner#.constrain#(Interloper#)
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /pcla` --- `fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(OT)` with `Readiness(
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
    1. `TypeVariable(PNT)` is `Readiness(
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
3. `TypeVariable(OT) == ScopeOwner` _from Fix variable OT_
4. Combine `TypeVariable(PNT) <: TypeVariable(OT)` with `TypeVariable(OT) == ScopeOwner`
    1. `TypeVariable(PNT) <: ScopeOwner`
5. Combine `TypeVariable(OT) == ScopeOwner` with `TypeVariable(PNT) <: CapturedType(in TypeVariable(OT))`
    1. `TypeVariable(PNT) <: CapturedType(in ScopeOwner)`
6. Choose `TypeVariable(PNT)` with `Readiness(
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
7. `TypeVariable(PNT) == ScopeOwner` _from Fix variable PNT_

### Call 16

```
pcla#(<L> = pcla@fun <implicit>.<anonymous>(otvOwner: <implicit>): <implicit> <inline=Unknown>  {
    lval pntvOwner: <implicit> = otvOwner#.createDerivativeTypeVariable#(otvOwner#.provideProjectedContainer#())
    otvOwner#.constrain#(ScopeOwner#())
    pntvOwner#.provide#().function#()
    otvOwner#.constrain#(Interloper#)
    pntvOwner#.constrain#(Interloper#)
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /pcla` --- `fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(OT)` for `FirNamedFunctionSymbol /pcla`s parameter 0

##### Resolution Stages > CheckArguments:

1. `(TypeVariableOwner<TypeVariable(OT)>) -> kotlin/Unit <: (TypeVariableOwner<TypeVariable(OT)>) -> kotlin/Unit` _from Argument <L> = pcla <implicit>.<anonymous>(otvOwner: <implicit>): <implicit> <inline=Unknown>  {↩    lval pntvOwner: <implicit> = otvOwner#.createDerivativeTypeVariable#(otvOwner#.provideProjectedContainer#())↩    otvOwner#.constrain#(ScopeOwner#())↩    pntvOwner#.provide#().function#()↩    otvOwner#.constrain#(Interloper#)↩    pntvOwner#.constrain#(Interloper#)↩}↩_

##### Call Completion:

1. Choose `TypeVariable(OT)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. Choose `TypeVariable(OT)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 17

```
R?C|<local>/otvOwner|.createDerivativeTypeVariable#(R?C|<local>/otvOwner|.R?C|/TypeVariableOwner.provideProjectedContainer|())
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable` --- `fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: ContravariantContainer<PNT>): TypeVariableOwner<PNT>↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(PNT)` for `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable`s parameter 0

##### Resolution Stages > CheckArguments:

1. `InvariantContainer<CapturedType(in TypeVariable(OT))> <: ContravariantContainer<TypeVariable(PNT)>` _from Argument R?C|<local>/otvOwner|.R?C|/TypeVariableOwner.provideProjectedContainer|()_
2. __NewConstraintError: `InvariantContainer<CapturedType(in TypeVariable(OT))> <: ContravariantContainer<TypeVariable(PNT)>`__

#### Candidate 2: `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable` --- `fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: InvariantContainer<in PNT>): TypeVariableOwner<PNT>↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(PNT)` for `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable`s parameter 0

##### Resolution Stages > CheckArguments:

1. `InvariantContainer<CapturedType(in TypeVariable(OT))> <: InvariantContainer<in TypeVariable(PNT)>` _from Argument R?C|<local>/otvOwner|.R?C|/TypeVariableOwner.provideProjectedContainer|()_
    1. `TypeVariable(PNT) <: TypeVariable(OT)`
    2. `TypeVariable(PNT) <: CapturedType(in TypeVariable(OT))`

##### Call Completion:

1. Choose `TypeVariable(PNT)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 18

```
R?C|<local>/otvOwner|.constrain#(R|/ScopeOwner.ScopeOwner|())
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.constrain` --- `fun constrain(subtypeValue: BaseType): Unit↩`
##### Resolution Stages > CheckArguments:

1. `ScopeOwner <: TypeVariable(OT)` _from Argument R|/ScopeOwner.ScopeOwner|()_

### Call 19

```
R?C|<local>/pntvOwner|.provide#()
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.provide` --- `fun provide(): BaseType↩`
##### Call Completion:

1. `TypeVariable(OT) == TypeVariable(PNT)` _from SimpleConstraintSystemConstraintPosition_
2. Combine `ScopeOwner <: TypeVariable(OT)` with `TypeVariable(OT) == TypeVariable(PNT)`
    1. `ScopeOwner <: TypeVariable(PNT)`
3. Combine `TypeVariable(OT) == TypeVariable(PNT)` with `TypeVariable(PNT) <: CapturedType(in TypeVariable(OT))`
    1. `TypeVariable(PNT) <: CapturedType(in TypeVariable(PNT))`
4. Combine `ScopeOwner <: TypeVariable(PNT)` with `TypeVariable(PNT) <: TypeVariable(OT)`
    1. `ScopeOwner <: TypeVariable(OT)`
5. `TypeVariable(PNT) == TypeVariable(OT)` _from Fix variable PNT_
6. Combine `TypeVariable(PNT) == TypeVariable(OT)` with `TypeVariable(PNT) <: CapturedType(in TypeVariable(OT))`
    1. `TypeVariable(OT) <: CapturedType(in TypeVariable(OT))`

### Call 20

```
R?C|<local>/otvOwner|.constrain#(Q|Interloper|)
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.constrain` --- `fun constrain(subtypeValue: BaseType): Unit↩`
##### Resolution Stages > CheckArguments:

1. `Interloper <: TypeVariable(OT)` _from Argument Q|Interloper|_
2. Combine `Interloper <: TypeVariable(OT)` with `TypeVariable(PNT) == TypeVariable(OT)`
    1. `Interloper <: TypeVariable(PNT)`

### Call 21

```
R?C|<local>/pntvOwner|.constrain#(Q|Interloper|)
```

#### Candidate 1: `FirNamedFunctionSymbol /TypeVariableOwner.constrain` --- `fun constrain(subtypeValue: BaseType): Unit↩`
##### Resolution Stages > CheckArguments:

1. `Interloper <: TypeVariable(PNT)` _from Argument Q|Interloper|_

### Call 16

```
pcla#(<L> = pcla@fun <implicit>.<anonymous>(otvOwner: <implicit>): <implicit> <inline=Unknown>  {
    lval pntvOwner: <implicit> = otvOwner#.createDerivativeTypeVariable#(otvOwner#.provideProjectedContainer#())
    otvOwner#.constrain#(ScopeOwner#())
    pntvOwner#.provide#().function#()
    otvOwner#.constrain#(Interloper#)
    pntvOwner#.constrain#(Interloper#)
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /pcla` --- `fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(OT)` with `Readiness(
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
    1. `TypeVariable(PNT)` is `Readiness(
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
3. `TypeVariable(OT) == BaseType` _from Fix variable OT_
4. Combine `TypeVariable(PNT) <: TypeVariable(OT)` with `TypeVariable(OT) == BaseType`
    1. `TypeVariable(PNT) <: BaseType`
5. Combine `TypeVariable(OT) == BaseType` with `TypeVariable(OT) <: CapturedType(in TypeVariable(OT))`
    1. `BaseType <: TypeVariable(OT)`
6. Combine `TypeVariable(OT) == BaseType` with `TypeVariable(PNT) <: CapturedType(in TypeVariable(OT))`
    1. `TypeVariable(PNT) <: CapturedType(in BaseType)`
7. Combine `TypeVariable(OT) == BaseType` with `TypeVariable(PNT) == TypeVariable(OT)`
    1. `BaseType <: TypeVariable(PNT)`
8. Combine `TypeVariable(OT) == BaseType` with `TypeVariable(OT) <: CapturedType(in TypeVariable(OT))`
    1. `TypeVariable(OT) <: CapturedType(in BaseType)`
9. Combine `TypeVariable(PNT) == TypeVariable(OT)` with `TypeVariable(PNT) <: BaseType`
    1. `TypeVariable(OT) <: BaseType`
10. Combine `TypeVariable(PNT) <: BaseType` with `BaseType <: TypeVariable(PNT)`
    1. `TypeVariable(PNT) == BaseType`
11. Choose `TypeVariable(PNT)` with `Readiness(
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

### Call 22

```
Null(null)!!
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/CHECK_NOT_NULL_CALL` --- `fun <K> CHECK_NOT_NULL_CALL(arg: K?): K & Any↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/CHECK_NOT_NULL_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Nothing? <: TypeVariable(K)?` _from Argument Null(null)_
    1. `kotlin/Nothing <: TypeVariable(K)`

##### Resolution Stages > CheckIncompatibleTypeVariableUpperBounds:

1. `TypeVariable(K) & Any <: T` _from ExpectedType for some call_
    1. `TypeVariable(K) <: T?`

##### Call Completion:

1. Choose `TypeVariable(K)` with `Readiness(
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
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(K) == kotlin/Nothing` _from Fix variable K_

### Call 23

```
ContravariantContainer#()
```

#### Candidate 1: `FirConstructorSymbol /ContravariantContainer.ContravariantContainer` --- `constructor<in CT>(): ContravariantContainer<CT>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(CT)` for `FirRegularClassSymbol ContravariantContainer`s parameter 0

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `ContravariantContainer<TypeVariable(CT)> <: ContravariantContainer<T>` _from ExpectedType for some call_
    1. `T <: TypeVariable(CT)`

##### Call Completion:

1. Choose `TypeVariable(CT)` with `Readiness(
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
2. `TypeVariable(CT) == T` _from Fix variable CT_

### Call 24

```
TypeVariableOwner#()
```

#### Candidate 1: `FirConstructorSymbol /TypeVariableOwner.TypeVariableOwner` --- `constructor<T>(): TypeVariableOwner<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirRegularClassSymbol TypeVariableOwner`s parameter 0

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `TypeVariableOwner<TypeVariable(T)> <: TypeVariableOwner<PNT>` _from ExpectedType for some call_
    1. `TypeVariable(T) <: PNT`
    2. `PNT <: TypeVariable(T)`
2. Combine `TypeVariable(T) <: PNT` with `PNT <: TypeVariable(T)`
    1. `TypeVariable(T) == PNT`

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
2. `TypeVariable(T) == PNT` _from Fix variable T_

### Call 25

```
InvariantContainer#()
```

#### Candidate 1: `FirConstructorSymbol /InvariantContainer.InvariantContainer` --- `constructor<CT>(): InvariantContainer<CT>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(CT)` for `FirRegularClassSymbol InvariantContainer`s parameter 0

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `InvariantContainer<TypeVariable(CT)> <: InvariantContainer<in T>` _from ExpectedType for some call_
    1. `T <: TypeVariable(CT)`

##### Call Completion:

1. Choose `TypeVariable(CT)` with `Readiness(
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
2. `TypeVariable(CT) == T` _from Fix variable CT_

### Call 26

```
TypeVariableOwner#()
```

#### Candidate 1: `FirConstructorSymbol /TypeVariableOwner.TypeVariableOwner` --- `constructor<T>(): TypeVariableOwner<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirRegularClassSymbol TypeVariableOwner`s parameter 0

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `TypeVariableOwner<TypeVariable(T)> <: TypeVariableOwner<PNT>` _from ExpectedType for some call_
    1. `TypeVariable(T) <: PNT`
    2. `PNT <: TypeVariable(T)`
2. Combine `TypeVariable(T) <: PNT` with `PNT <: TypeVariable(T)`
    1. `TypeVariable(T) == PNT`

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
2. `TypeVariable(T) == PNT` _from Fix variable T_

### Call 27

```
Null(null)!!
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/CHECK_NOT_NULL_CALL` --- `fun <K> CHECK_NOT_NULL_CALL(arg: K?): K & Any↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/CHECK_NOT_NULL_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Nothing? <: TypeVariable(K)?` _from Argument Null(null)_
    1. `kotlin/Nothing <: TypeVariable(K)`

##### Resolution Stages > CheckIncompatibleTypeVariableUpperBounds:

1. `TypeVariable(K) & Any <: OT` _from ExpectedType for some call_
    1. `TypeVariable(K) <: OT?`

##### Call Completion:

1. Choose `TypeVariable(K)` with `Readiness(
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
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(K) == kotlin/Nothing` _from Fix variable K_

##### Some isEquallyOrMoreSpecific() call:

1. New `TypeVariable(PNT)` for `FirNamedFunctionSymbol /TypeVariableOwner.createDerivativeTypeVariable`s parameter 0
2. `TypeVariable(PNT) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `InvariantContainer<CapturedType(in PNT)> <: ContravariantContainer<TypeVariable(PNT)>` _from SimpleConstraintSystemConstraintPosition_
4. __NewConstraintError: `InvariantContainer<CapturedType(in PNT)> <: ContravariantContainer<TypeVariable(PNT)>`__