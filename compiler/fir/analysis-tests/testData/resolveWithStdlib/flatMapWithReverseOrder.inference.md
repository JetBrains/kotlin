## `Source session for module <main>`

### Call 1

```
mutableSetOf#<R|kotlin/Int|>()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/mutableSetOf` --- `@SinceKotlin(...) @InlineOnly() fun <T> mutableSetOf(): MutableSet<T>↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/mutableSetOf`s parameter 0
2. `TypeVariable(T) == kotlin/Int` _from TypeParameter R|kotlin/Int|_

#### Candidate 2: `FirNamedFunctionSymbol kotlin/collections/mutableSetOf` --- `fun <T> mutableSetOf(vararg elements: T): MutableSet<T>↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/mutableSetOf`s parameter 0
2. `TypeVariable(T) == kotlin/Int` _from TypeParameter R|kotlin/Int|_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/mutableSetOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/mutableSetOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_

### Call 1

```
mutableSetOf#<R|kotlin/Int|>()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/mutableSetOf` --- `@SinceKotlin(...) @InlineOnly() fun <T> mutableSetOf(): MutableSet<T>↩`
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
2. `TypeVariable(T) == kotlin/Int` _from Fix variable T_

### Call 2

```
Q|Wrapper|.reverseOrder#()
```

#### Candidate 1: `FirNamedFunctionSymbol /Wrapper.reverseOrder` --- `static fun <W : Comparable<in W!>!> reverseOrder(): Comparator<W!>!↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(W)` for `FirNamedFunctionSymbol /Wrapper.reverseOrder`s parameter 0
2. `TypeVariable(W) <: kotlin/Comparable<in TypeVariable(W)!>!` _from DeclaredUpperBound_

##### Call Completion:

1. Choose `TypeVariable(W)` with `Readiness(
   	false ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 3

```
TreeSet#(Q|Wrapper|.R?C|/Wrapper.reverseOrder|())
```

#### Candidate 1: `FirConstructorSymbol java/util/TreeSet.TreeSet` --- `constructor<E : Any!>(p0: Comparator<in E!>!): TreeSet<E>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E)` for `FirRegularClassSymbol java/util/TreeSet`s parameter 0

##### Resolution Stages > CheckArguments:

1. `java/util/Comparator<TypeVariable(W)!>! <: java/util/Comparator<in TypeVariable(E)!>!` _from SimpleConstraintSystemConstraintPosition_
    1. `ft<TypeVariable(E) & Any, TypeVariable(E)?> <: TypeVariable(W)`
    2. `TypeVariable(E) <: TypeVariable(W)!`
2. Combine `ft<TypeVariable(E) & Any, TypeVariable(E)?> <: TypeVariable(W)` with `TypeVariable(W) <: kotlin/Comparable<in TypeVariable(W)!>!`
    1. `TypeVariable(E) <: kotlin/Comparable<in TypeVariable(W)!>?`
    2. `TypeVariable(W) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>!`
3. Combine `ft<TypeVariable(E) & Any, TypeVariable(E)?> <: TypeVariable(W)` with `TypeVariable(W) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>!`
    1. `TypeVariable(E) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>?`

#### Candidate 2: `FirConstructorSymbol java/util/TreeSet.TreeSet` --- `constructor<E : Any!>(p0: ft<MutableCollection<out E!>, Collection<out E!>?>): TreeSet<E>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E)` for `FirRegularClassSymbol java/util/TreeSet`s parameter 0

##### Resolution Stages > CheckArguments:

1. `java/util/Comparator<TypeVariable(W)!>! <: ft<kotlin/collections/MutableCollection<out TypeVariable(E)!>, kotlin/collections/Collection<out TypeVariable(E)!>?>` _from Argument Q|Wrapper|.R?C|/Wrapper.reverseOrder|()_
2. __NewConstraintError: `java/util/Comparator<TypeVariable(W)!>! <: ft<kotlin/collections/MutableCollection<out TypeVariable(E)!>, kotlin/collections/Collection<out TypeVariable(E)!>?>`__

#### Candidate 3: `FirConstructorSymbol java/util/TreeSet.TreeSet` --- `constructor<E : Any!>(p0: SortedSet<E!>!): TreeSet<E>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E)` for `FirRegularClassSymbol java/util/TreeSet`s parameter 0

##### Resolution Stages > CheckArguments:

1. `java/util/Comparator<TypeVariable(W)!>! <: java/util/SortedSet<TypeVariable(E)!>!` _from Argument Q|Wrapper|.R?C|/Wrapper.reverseOrder|()_
2. __NewConstraintError: `java/util/Comparator<TypeVariable(W)!>! <: java/util/SortedSet<TypeVariable(E)!>!`__

#### Candidate 1: `FirConstructorSymbol java/util/TreeSet.TreeSet` --- `constructor<E : Any!>(p0: Comparator<in E!>!): TreeSet<E>`
##### Call Completion:

1. Choose `TypeVariable(E)` with `Readiness(
   	false ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(W)` is `Readiness(
       	false ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`

### Call 4

```
R|<local>/fragments|.flatMapTo#(R?C|java/util/TreeSet.TreeSet|(Q|Wrapper|.R?C|/Wrapper.reverseOrder|()), <L> = flatMapTo@fun <implicit>.<anonymous>(f: <implicit>): <implicit> <inline=Unknown>  {
    f#.tailsAndBody#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/flatMapTo` --- `@IgnorableReturnValue() fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapTo(destination: C, transform: (T) -> Iterable<R>): C↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/flatMapTo`s parameter 0
2. New `TypeVariable(R)` for `FirNamedFunctionSymbol kotlin/collections/flatMapTo`s parameter 1
3. New `TypeVariable(C)` for `FirNamedFunctionSymbol kotlin/collections/flatMapTo`s parameter 2
4. `TypeVariable(C) <: kotlin/collections/MutableCollection<in TypeVariable(R)>` _from DeclaredUpperBound_

##### Resolution Stages > CheckExtensionReceiver:

1. `kotlin/collections/Set<MergeFragment> <: kotlin/collections/Iterable<TypeVariable(T)>` _from Receiver R|<local>/fragments|_
    1. `MergeFragment <: TypeVariable(T)`

##### Resolution Stages > CheckArguments:

1. `java/util/TreeSet<TypeVariable(E)> <: TypeVariable(C)` _from Argument R?C|java/util/TreeSet.TreeSet|(Q|Wrapper|.R?C|/Wrapper.reverseOrder|())_
2. Combine `java/util/TreeSet<TypeVariable(E)> <: TypeVariable(C)` with `TypeVariable(C) <: kotlin/collections/MutableCollection<in TypeVariable(R)>`
    1. `TypeVariable(R) <: TypeVariable(E)`
3. Combine `TypeVariable(R) <: TypeVariable(E)` with `TypeVariable(E) <: TypeVariable(W)!`
    1. `TypeVariable(R)! <: TypeVariable(W)`
    2. `TypeVariable(R) <: TypeVariable(W)!`
4. Combine `TypeVariable(R) <: TypeVariable(E)` with `TypeVariable(E) <: kotlin/Comparable<in TypeVariable(W)!>?`
    1. `TypeVariable(R) <: kotlin/Comparable<in TypeVariable(W)!>?`
5. Combine `TypeVariable(R) <: TypeVariable(E)` with `TypeVariable(E) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>?`
    1. `TypeVariable(R) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>?`
6. Combine `TypeVariable(R) <: TypeVariable(E)` with `ft<TypeVariable(E) & Any, TypeVariable(E)?> <: TypeVariable(W)`
    1. `ft<TypeVariable(R) & Any, TypeVariable(R)?> <: TypeVariable(W)`
7. Combine `TypeVariable(R) <: TypeVariable(E)` with `TypeVariable(W) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>!`
    1. `TypeVariable(W) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>!`
8. Combine `TypeVariable(R) <: TypeVariable(E)` with `TypeVariable(E) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>?`
    1. `TypeVariable(E) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>?`
9. Combine `TypeVariable(R)! <: TypeVariable(W)` with `TypeVariable(W) <: kotlin/Comparable<in TypeVariable(W)!>!`
    1. `TypeVariable(R) <: kotlin/Comparable<in TypeVariable(W)!>!`
10. Combine `TypeVariable(R)! <: TypeVariable(W)` with `TypeVariable(W) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>!`
    1. `TypeVariable(R) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>!`
11. Combine `TypeVariable(R)! <: TypeVariable(W)` with `TypeVariable(W) <: kotlin/Comparable<in TypeVariable(W)!>!`
    1. `TypeVariable(W) <: kotlin/Comparable<TypeVariable(R)!>!`
12. Combine `TypeVariable(R)! <: TypeVariable(W)` with `TypeVariable(E) <: kotlin/Comparable<in TypeVariable(W)!>?`
    1. `TypeVariable(E) <: kotlin/Comparable<TypeVariable(R)!>?`
13. Combine `TypeVariable(R)! <: TypeVariable(W)` with `TypeVariable(W) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>!`
    1. `TypeVariable(R) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>!`
14. Combine `TypeVariable(R) <: TypeVariable(E)` with `TypeVariable(E) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>?`
    1. `TypeVariable(R) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>?`
15. Combine `TypeVariable(R)! <: TypeVariable(W)` with `TypeVariable(W) <: kotlin/Comparable<TypeVariable(R)!>!`
    1. `TypeVariable(R) <: kotlin/Comparable<TypeVariable(R)!>!`
16. Combine `TypeVariable(R) <: TypeVariable(E)` with `TypeVariable(E) <: kotlin/Comparable<TypeVariable(R)!>?`
    1. `TypeVariable(R) <: kotlin/Comparable<TypeVariable(R)!>?`
17. `(TypeVariable(T)) -> kotlin/collections/Iterable<TypeVariable(R)> <: (TypeVariable(T)) -> kotlin/collections/Iterable<TypeVariable(R)>` _from Argument <L> = flatMapTo <implicit>.<anonymous>(f: <implicit>): <implicit> <inline=Unknown>  {↩    f#.tailsAndBody#↩}↩_

#### Candidate 2: `FirNamedFunctionSymbol kotlin/collections/flatMapTo` --- `@SinceKotlin(...) @OverloadResolutionByLambdaReturnType() @JvmName(...) @IgnorableReturnValue() fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapTo(destination: C, transform: (T) -> Sequence<R>): C↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/flatMapTo`s parameter 0
2. New `TypeVariable(R)` for `FirNamedFunctionSymbol kotlin/collections/flatMapTo`s parameter 1
3. New `TypeVariable(C)` for `FirNamedFunctionSymbol kotlin/collections/flatMapTo`s parameter 2
4. `TypeVariable(C) <: kotlin/collections/MutableCollection<in TypeVariable(R)>` _from DeclaredUpperBound_

##### Resolution Stages > CheckExtensionReceiver:

1. `kotlin/collections/Set<MergeFragment> <: kotlin/collections/Iterable<TypeVariable(T)>` _from Receiver R|<local>/fragments|_
    1. `MergeFragment <: TypeVariable(T)`

##### Resolution Stages > CheckArguments:

1. `java/util/TreeSet<TypeVariable(E)> <: TypeVariable(C)` _from Argument R?C|java/util/TreeSet.TreeSet|(Q|Wrapper|.R?C|/Wrapper.reverseOrder|())_
2. Combine `java/util/TreeSet<TypeVariable(E)> <: TypeVariable(C)` with `TypeVariable(C) <: kotlin/collections/MutableCollection<in TypeVariable(R)>`
    1. `TypeVariable(R) <: TypeVariable(E)`
3. Combine `TypeVariable(R) <: TypeVariable(E)` with `TypeVariable(E) <: TypeVariable(W)!`
    1. `TypeVariable(R)! <: TypeVariable(W)`
    2. `TypeVariable(R) <: TypeVariable(W)!`
4. Combine `TypeVariable(R) <: TypeVariable(E)` with `TypeVariable(E) <: kotlin/Comparable<in TypeVariable(W)!>?`
    1. `TypeVariable(R) <: kotlin/Comparable<in TypeVariable(W)!>?`
5. Combine `TypeVariable(R) <: TypeVariable(E)` with `TypeVariable(E) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>?`
    1. `TypeVariable(R) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>?`
6. Combine `TypeVariable(R) <: TypeVariable(E)` with `ft<TypeVariable(E) & Any, TypeVariable(E)?> <: TypeVariable(W)`
    1. `ft<TypeVariable(R) & Any, TypeVariable(R)?> <: TypeVariable(W)`
7. Combine `TypeVariable(R) <: TypeVariable(E)` with `TypeVariable(W) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>!`
    1. `TypeVariable(W) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>!`
8. Combine `TypeVariable(R) <: TypeVariable(E)` with `TypeVariable(E) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>?`
    1. `TypeVariable(E) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>?`
9. Combine `TypeVariable(R)! <: TypeVariable(W)` with `TypeVariable(W) <: kotlin/Comparable<in TypeVariable(W)!>!`
    1. `TypeVariable(R) <: kotlin/Comparable<in TypeVariable(W)!>!`
10. Combine `TypeVariable(R)! <: TypeVariable(W)` with `TypeVariable(W) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>!`
    1. `TypeVariable(R) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>!`
11. Combine `TypeVariable(R)! <: TypeVariable(W)` with `TypeVariable(W) <: kotlin/Comparable<in TypeVariable(W)!>!`
    1. `TypeVariable(W) <: kotlin/Comparable<TypeVariable(R)!>!`
12. Combine `TypeVariable(R)! <: TypeVariable(W)` with `TypeVariable(E) <: kotlin/Comparable<in TypeVariable(W)!>?`
    1. `TypeVariable(E) <: kotlin/Comparable<TypeVariable(R)!>?`
13. Combine `TypeVariable(R)! <: TypeVariable(W)` with `TypeVariable(W) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>!`
    1. `TypeVariable(R) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>!`
14. Combine `TypeVariable(R) <: TypeVariable(E)` with `TypeVariable(E) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>?`
    1. `TypeVariable(R) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>?`
15. Combine `TypeVariable(R)! <: TypeVariable(W)` with `TypeVariable(W) <: kotlin/Comparable<TypeVariable(R)!>!`
    1. `TypeVariable(R) <: kotlin/Comparable<TypeVariable(R)!>!`
16. Combine `TypeVariable(R) <: TypeVariable(E)` with `TypeVariable(E) <: kotlin/Comparable<TypeVariable(R)!>?`
    1. `TypeVariable(R) <: kotlin/Comparable<TypeVariable(R)!>?`
17. `(TypeVariable(T)) -> kotlin/sequences/Sequence<TypeVariable(R)> <: (TypeVariable(T)) -> kotlin/sequences/Sequence<TypeVariable(R)>` _from Argument <L> = flatMapTo <implicit>.<anonymous>(f: <implicit>): <implicit> <inline=Unknown>  {↩    f#.tailsAndBody#↩}↩_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/flatMapTo`s parameter 0
2. New `TypeVariable(R)` for `FirNamedFunctionSymbol kotlin/collections/flatMapTo`s parameter 1
3. New `TypeVariable(C)` for `FirNamedFunctionSymbol kotlin/collections/flatMapTo`s parameter 2
4. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
5. `TypeVariable(R) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
6. `TypeVariable(C) <: kotlin/collections/MutableCollection<in TypeVariable(R)>` _from SimpleConstraintSystemConstraintPosition_
7. `kotlin/collections/Iterable<T> <: kotlin/collections/Iterable<TypeVariable(T)>` _from SimpleConstraintSystemConstraintPosition_
    1. `T <: TypeVariable(T)`
8. `C <: TypeVariable(C)` _from SimpleConstraintSystemConstraintPosition_
9. Combine `C <: TypeVariable(C)` with `TypeVariable(C) <: kotlin/collections/MutableCollection<in TypeVariable(R)>`
    1. `TypeVariable(R) <: R`
10. `(T) -> kotlin/collections/Iterable<R> <: (TypeVariable(T)) -> kotlin/sequences/Sequence<TypeVariable(R)>` _from SimpleConstraintSystemConstraintPosition_
    1. `TypeVariable(T) <: T`
11. __NewConstraintError: `(T) -> kotlin/collections/Iterable<R> <: (TypeVariable(T)) -> kotlin/sequences/Sequence<TypeVariable(R)>`__
12. Combine `T <: TypeVariable(T)` with `TypeVariable(T) <: T`
    1. `TypeVariable(T) == T`

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/flatMapTo`s parameter 0
2. New `TypeVariable(R)` for `FirNamedFunctionSymbol kotlin/collections/flatMapTo`s parameter 1
3. New `TypeVariable(C)` for `FirNamedFunctionSymbol kotlin/collections/flatMapTo`s parameter 2
4. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
5. `TypeVariable(R) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
6. `TypeVariable(C) <: kotlin/collections/MutableCollection<in TypeVariable(R)>` _from SimpleConstraintSystemConstraintPosition_
7. `kotlin/collections/Iterable<T> <: kotlin/collections/Iterable<TypeVariable(T)>` _from SimpleConstraintSystemConstraintPosition_
    1. `T <: TypeVariable(T)`
8. `C <: TypeVariable(C)` _from SimpleConstraintSystemConstraintPosition_
9. Combine `C <: TypeVariable(C)` with `TypeVariable(C) <: kotlin/collections/MutableCollection<in TypeVariable(R)>`
    1. `TypeVariable(R) <: R`
10. `(T) -> kotlin/sequences/Sequence<R> <: (TypeVariable(T)) -> kotlin/collections/Iterable<TypeVariable(R)>` _from SimpleConstraintSystemConstraintPosition_
    1. `TypeVariable(T) <: T`
11. __NewConstraintError: `(T) -> kotlin/sequences/Sequence<R> <: (TypeVariable(T)) -> kotlin/collections/Iterable<TypeVariable(R)>`__
12. Combine `T <: TypeVariable(T)` with `TypeVariable(T) <: T`
    1. `TypeVariable(T) == T`

### Call 4

```
R|<local>/fragments|.flatMapTo#(R?C|java/util/TreeSet.TreeSet|(Q|Wrapper|.R?C|/Wrapper.reverseOrder|()), <L> = flatMapTo@fun <implicit>.<anonymous>(f: <implicit>): <implicit> <inline=Unknown>  {
    f#.tailsAndBody#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/flatMapTo` --- `@IgnorableReturnValue() fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapTo(destination: C, transform: (T) -> Iterable<R>): C↩`
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
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(R)` is `Readiness(
       	 true ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    2. `TypeVariable(C)` is `Readiness(
       	 true ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    3. `TypeVariable(E)` is `Readiness(
       	 true ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    4. `TypeVariable(W)` is `Readiness(
       	 true ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(T) == MergeFragment` _from Fix variable T_

#### Candidate 2: `FirNamedFunctionSymbol kotlin/collections/flatMapTo` --- `@SinceKotlin(...) @OverloadResolutionByLambdaReturnType() @JvmName(...) @IgnorableReturnValue() fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapTo(destination: C, transform: (T) -> Sequence<R>): C↩`
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
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(R)` is `Readiness(
       	 true ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    2. `TypeVariable(C)` is `Readiness(
       	 true ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    3. `TypeVariable(E)` is `Readiness(
       	 true ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    4. `TypeVariable(W)` is `Readiness(
       	 true ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(T) == MergeFragment` _from Fix variable T_

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/flatMapTo` --- `@IgnorableReturnValue() fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapTo(destination: C, transform: (T) -> Iterable<R>): C↩`
##### Continue Call Completion:

1. New `TypeVariable(_R)` for lambda return type
2. `(MergeFragment) -> TypeVariable(_R) <: (MergeFragment) -> kotlin/collections/Iterable<TypeVariable(R)>` _from Argument flatMapTo <implicit>.<anonymous>(f: <implicit>): <implicit> <inline=Unknown>  {↩    f#.tailsAndBody#↩}↩_
    1. `TypeVariable(_R) <: kotlin/collections/Iterable<TypeVariable(R)>`

#### Candidate 2: `FirNamedFunctionSymbol kotlin/collections/flatMapTo` --- `@SinceKotlin(...) @OverloadResolutionByLambdaReturnType() @JvmName(...) @IgnorableReturnValue() fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapTo(destination: C, transform: (T) -> Sequence<R>): C↩`
##### Continue Call Completion:

1. New `TypeVariable(_R)` for lambda return type
2. `(MergeFragment) -> TypeVariable(_R) <: (MergeFragment) -> kotlin/sequences/Sequence<TypeVariable(R)>` _from Argument flatMapTo <implicit>.<anonymous>(f: <implicit>): <implicit> <inline=Unknown>  {↩    f#.tailsAndBody#↩}↩_
    1. `TypeVariable(_R) <: kotlin/sequences/Sequence<TypeVariable(R)>`

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/flatMapTo` --- `@IgnorableReturnValue() fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapTo(destination: C, transform: (T) -> Iterable<R>): C↩`
##### Continue Continue Call Completion:

1. `kotlin/collections/MutableSet<kotlin/Int> <: TypeVariable(_R)` _from LambdaArgument_
2. Combine `kotlin/collections/MutableSet<kotlin/Int> <: TypeVariable(_R)` with `TypeVariable(_R) <: kotlin/collections/Iterable<TypeVariable(R)>`
    1. `kotlin/Int <: TypeVariable(R)`
3. Combine `kotlin/Int <: TypeVariable(R)` with `TypeVariable(R) <: TypeVariable(E)`
    1. `kotlin/Int <: TypeVariable(E)`
4. Combine `kotlin/Int <: TypeVariable(R)` with `TypeVariable(R) <: TypeVariable(W)!`
    1. `kotlin/Int! <: TypeVariable(W)`
5. Combine `kotlin/Int <: TypeVariable(R)` with `TypeVariable(R) <: kotlin/Comparable<in TypeVariable(W)!>?`
    1. `TypeVariable(W) <: kotlin/Int!`
6. Combine `kotlin/Int <: TypeVariable(R)` with `TypeVariable(R) <: kotlin/Comparable<ft<TypeVariable(E) & Any, TypeVariable(E)?>>?`
    1. `TypeVariable(E) <: kotlin/Int?`
7. Combine `kotlin/Int <: TypeVariable(R)` with `TypeVariable(R) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>!`
    1. `TypeVariable(R) <: kotlin/Int?`
8. Combine `kotlin/Int <: TypeVariable(R)` with `TypeVariable(R) <: kotlin/Comparable<TypeVariable(R)!>!`
    1. `TypeVariable(R) <: kotlin/Int!`
9. Combine `kotlin/Int <: TypeVariable(R)` with `TypeVariable(E) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>?`
    1. `TypeVariable(E) <: kotlin/Comparable<kotlin/Int!>?`
10. Combine `kotlin/Int <: TypeVariable(R)` with `TypeVariable(W) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>!`
    1. `TypeVariable(W) <: kotlin/Comparable<kotlin/Int!>!`
11. Combine `kotlin/Int <: TypeVariable(R)` with `TypeVariable(R) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>!`
    1. `TypeVariable(R) <: kotlin/Comparable<kotlin/Int!>!`
12. Combine `kotlin/Int <: TypeVariable(R)` with `TypeVariable(R) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>?`
    1. `TypeVariable(R) <: kotlin/Comparable<kotlin/Int!>?`
13. Combine `kotlin/Int! <: TypeVariable(W)` with `TypeVariable(W) <: kotlin/Int!`
    1. `TypeVariable(W) == kotlin/Int!`
14. Combine `TypeVariable(W) == kotlin/Int!` with `TypeVariable(W) <: kotlin/Comparable<in TypeVariable(W)!>!`
    1. `TypeVariable(W) <: kotlin/Comparable<in kotlin/Int!>!`
15. Combine `TypeVariable(W) == kotlin/Int!` with `TypeVariable(E) <: TypeVariable(W)!`
    1. `TypeVariable(E) <: kotlin/Int!`
16. Combine `TypeVariable(W) == kotlin/Int!` with `TypeVariable(E) <: kotlin/Comparable<in TypeVariable(W)!>?`
    1. `TypeVariable(E) <: kotlin/Comparable<in kotlin/Int!>?`
17. Combine `TypeVariable(W) == kotlin/Int!` with `TypeVariable(R) <: kotlin/Comparable<in TypeVariable(W)!>?`
    1. `TypeVariable(R) <: kotlin/Comparable<in kotlin/Int!>?`
18. Combine `TypeVariable(W) == kotlin/Int!` with `TypeVariable(R) <: kotlin/Comparable<in TypeVariable(W)!>!`
    1. `TypeVariable(R) <: kotlin/Comparable<in kotlin/Int!>!`
19. Combine `TypeVariable(R) <: kotlin/Int?` with `TypeVariable(_R) <: kotlin/collections/Iterable<TypeVariable(R)>`
    1. `TypeVariable(_R) <: kotlin/collections/Iterable<kotlin/Int?>`
20. Combine `TypeVariable(R) <: kotlin/Int!` with `TypeVariable(_R) <: kotlin/collections/Iterable<TypeVariable(R)>`
    1. `TypeVariable(_R) <: kotlin/collections/Iterable<kotlin/Int!>`
21. Combine `TypeVariable(R) <: kotlin/Comparable<in kotlin/Int!>?` with `TypeVariable(_R) <: kotlin/collections/Iterable<TypeVariable(R)>`
    1. `TypeVariable(_R) <: kotlin/collections/Iterable<kotlin/Comparable<in kotlin/Int!>?>`
22. Combine `TypeVariable(R) <: kotlin/Comparable<in kotlin/Int!>!` with `TypeVariable(_R) <: kotlin/collections/Iterable<TypeVariable(R)>`
    1. `TypeVariable(_R) <: kotlin/collections/Iterable<kotlin/Comparable<in kotlin/Int!>!>`

#### Candidate 2: `FirNamedFunctionSymbol kotlin/collections/flatMapTo` --- `@SinceKotlin(...) @OverloadResolutionByLambdaReturnType() @JvmName(...) @IgnorableReturnValue() fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapTo(destination: C, transform: (T) -> Sequence<R>): C↩`
##### Continue Continue Call Completion:

1. `kotlin/collections/MutableSet<kotlin/Int> <: TypeVariable(_R)` _from LambdaArgument_
2. __NewConstraintError: `kotlin/collections/MutableSet<kotlin/Int> <: kotlin/sequences/Sequence<TypeVariable(R)>`__

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/flatMapTo` --- `@IgnorableReturnValue() fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapTo(destination: C, transform: (T) -> Iterable<R>): C↩`
##### Call Completion:

1. Choose `TypeVariable(W)` with `Readiness(
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
    1. `TypeVariable(R)` is `Readiness(
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
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    2. `TypeVariable(C)` is `Readiness(
       	 true ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    3. `TypeVariable(E)` is `Readiness(
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
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    4. `TypeVariable(_R)` is `Readiness(
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
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(W) == kotlin/Int!` _from Fix variable W_
3. Combine `ft<TypeVariable(E) & Any, TypeVariable(E)?> <: TypeVariable(W)` with `TypeVariable(W) == kotlin/Int!`
    1. `TypeVariable(E) <: kotlin/Int?`
4. Combine `TypeVariable(R)! <: TypeVariable(W)` with `TypeVariable(W) == kotlin/Int!`
    1. `TypeVariable(R) <: kotlin/Int!`
5. Combine `TypeVariable(W) == kotlin/Int!` with `TypeVariable(W) <: kotlin/Comparable<in TypeVariable(W)!>!`
    1. `TypeVariable(W) <: kotlin/Int!`
6. Combine `TypeVariable(W) == kotlin/Int!` with `TypeVariable(W) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>!`
    1. `TypeVariable(R) <: kotlin/Int?`
7. Combine `TypeVariable(W) == kotlin/Int!` with `TypeVariable(W) <: kotlin/Comparable<in TypeVariable(W)!>!`
    1. `TypeVariable(W) <: kotlin/Comparable<in kotlin/Int!>!`
8. Combine `TypeVariable(W) == kotlin/Int!` with `TypeVariable(E) <: TypeVariable(W)!`
    1. `TypeVariable(E) <: kotlin/Int!`
9. Combine `TypeVariable(W) == kotlin/Int!` with `TypeVariable(E) <: kotlin/Comparable<in TypeVariable(W)!>?`
    1. `TypeVariable(E) <: kotlin/Comparable<in kotlin/Int!>?`
10. Combine `TypeVariable(W) == kotlin/Int!` with `TypeVariable(R) <: kotlin/Comparable<in TypeVariable(W)!>?`
    1. `TypeVariable(R) <: kotlin/Comparable<in kotlin/Int!>?`
11. Combine `TypeVariable(W) == kotlin/Int!` with `TypeVariable(R) <: kotlin/Comparable<in TypeVariable(W)!>!`
    1. `TypeVariable(R) <: kotlin/Comparable<in kotlin/Int!>!`
12. Choose `TypeVariable(R)` with `Readiness(
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
    	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
    )`
    1. `TypeVariable(C)` is `Readiness(
       	 true ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    2. `TypeVariable(E)` is `Readiness(
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
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    3. `TypeVariable(_R)` is `Readiness(
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
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
13. `TypeVariable(R) == kotlin/Int` _from Fix variable R_
14. Combine `TypeVariable(R) == kotlin/Int` with `TypeVariable(R) <: TypeVariable(E)`
    1. `kotlin/Int <: TypeVariable(E)`
15. Combine `TypeVariable(R) == kotlin/Int` with `TypeVariable(C) <: kotlin/collections/MutableCollection<in TypeVariable(R)>`
    1. `TypeVariable(C) <: kotlin/collections/MutableCollection<in kotlin/Int>`
16. Combine `TypeVariable(R) == kotlin/Int` with `TypeVariable(E) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>?`
    1. `TypeVariable(E) <: kotlin/Comparable<kotlin/Int!>?`
17. Combine `TypeVariable(R) == kotlin/Int` with `TypeVariable(R) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>!`
    1. `TypeVariable(R) <: kotlin/Comparable<kotlin/Int!>!`
18. Combine `TypeVariable(R) == kotlin/Int` with `TypeVariable(R) <: kotlin/Comparable<ft<TypeVariable(R) & Any, TypeVariable(R)?>>?`
    1. `TypeVariable(R) <: kotlin/Comparable<kotlin/Int!>?`
19. Combine `TypeVariable(R) == kotlin/Int` with `TypeVariable(_R) <: kotlin/collections/Iterable<TypeVariable(R)>`
    1. `TypeVariable(_R) <: kotlin/collections/Iterable<kotlin/Int>`
20. Choose `TypeVariable(E)` with `Readiness(
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
    1. `TypeVariable(C)` is `Readiness(
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
    2. `TypeVariable(_R)` is `Readiness(
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
21. `TypeVariable(E) == kotlin/Int` _from Fix variable E_
22. Combine `TypeVariable(E) == kotlin/Int` with `java/util/TreeSet<TypeVariable(E)> <: TypeVariable(C)`
    1. `java/util/TreeSet<kotlin/Int> <: TypeVariable(C)`
23. Choose `TypeVariable(C)` with `Readiness(
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
    1. `TypeVariable(_R)` is `Readiness(
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
24. `TypeVariable(C) == java/util/TreeSet<kotlin/Int>` _from Fix variable C_
25. Choose `TypeVariable(_R)` with `Readiness(
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
26. `TypeVariable(_R) == kotlin/collections/MutableSet<kotlin/Int>` _from Fix variable _R_

### Call 5

```
R|<local>/<iterator>|.hasNext#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/MutableIterator.hasNext` --- `fun hasNext(): Boolean↩`
##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Boolean <: kotlin/Boolean` _from ExpectedType for some call_

### Call 6

```
testFun#(R|<local>/f|)
```

#### Candidate 1: `FirNamedFunctionSymbol /testFun` --- `fun testFun(i: Int): Unit↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/Int <: kotlin/Int` _from Argument R|<local>/f|_