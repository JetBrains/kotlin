## `Source session for module <main>`

### Call 1

```
bar#(R|<local>/generic|, R|<local>/first|, R|<local>/second|, R|<local>/third|, R|<local>/fourth|, R|<local>/fifth|)
```

#### Candidate 1: `FirNamedFunctionSymbol /bar` --- `fun <U0 : Generic<U1, U2, U3, U4, U5, U6, U7, U8, U9, Ua, Ub, Uc, Ud, Ue, Uf>, U1 : Alpha, U2 : Beta, U3 : Gamma, U4 : Delta, U5 : Epsilon, U6 : Psi, U7 : Omega, U8 : Sigma, U9 : Rho, Ua : Mu, Ub : Nu, Uc : Pi, Ud : Dzeta, Ue : Teta, Uf : Jot> bar(generic: U0, first: U1.(U2) -> U3, second: U4.(U5) -> U6, third: U7.(U8) -> U9, fourth: Ua.(Ub) -> Uc, fifth: Ud.(Ue) -> Uf): Any↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(U0)` for `FirNamedFunctionSymbol /bar`s parameter 0
2. New `TypeVariable(U1)` for `FirNamedFunctionSymbol /bar`s parameter 1
3. New `TypeVariable(U2)` for `FirNamedFunctionSymbol /bar`s parameter 2
4. New `TypeVariable(U3)` for `FirNamedFunctionSymbol /bar`s parameter 3
5. New `TypeVariable(U4)` for `FirNamedFunctionSymbol /bar`s parameter 4
6. New `TypeVariable(U5)` for `FirNamedFunctionSymbol /bar`s parameter 5
7. New `TypeVariable(U6)` for `FirNamedFunctionSymbol /bar`s parameter 6
8. New `TypeVariable(U7)` for `FirNamedFunctionSymbol /bar`s parameter 7
9. New `TypeVariable(U8)` for `FirNamedFunctionSymbol /bar`s parameter 8
10. New `TypeVariable(U9)` for `FirNamedFunctionSymbol /bar`s parameter 9
11. New `TypeVariable(Ua)` for `FirNamedFunctionSymbol /bar`s parameter 10
12. New `TypeVariable(Ub)` for `FirNamedFunctionSymbol /bar`s parameter 11
13. New `TypeVariable(Uc)` for `FirNamedFunctionSymbol /bar`s parameter 12
14. New `TypeVariable(Ud)` for `FirNamedFunctionSymbol /bar`s parameter 13
15. New `TypeVariable(Ue)` for `FirNamedFunctionSymbol /bar`s parameter 14
16. New `TypeVariable(Uf)` for `FirNamedFunctionSymbol /bar`s parameter 15
17. `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>` _from DeclaredUpperBound_
18. `TypeVariable(U1) <: Alpha` _from DeclaredUpperBound_
19. `TypeVariable(U2) <: Beta` _from DeclaredUpperBound_
20. `TypeVariable(U3) <: Gamma` _from DeclaredUpperBound_
21. `TypeVariable(U4) <: Delta` _from DeclaredUpperBound_
22. `TypeVariable(U5) <: Epsilon` _from DeclaredUpperBound_
23. `TypeVariable(U6) <: Psi` _from DeclaredUpperBound_
24. `TypeVariable(U7) <: Omega` _from DeclaredUpperBound_
25. `TypeVariable(U8) <: Sigma` _from DeclaredUpperBound_
26. `TypeVariable(U9) <: Rho` _from DeclaredUpperBound_
27. `TypeVariable(Ua) <: Mu` _from DeclaredUpperBound_
28. `TypeVariable(Ub) <: Nu` _from DeclaredUpperBound_
29. `TypeVariable(Uc) <: Pi` _from DeclaredUpperBound_
30. `TypeVariable(Ud) <: Dzeta` _from DeclaredUpperBound_
31. `TypeVariable(Ue) <: Teta` _from DeclaredUpperBound_
32. `TypeVariable(Uf) <: Jot` _from DeclaredUpperBound_

##### Resolution Stages > CheckArguments:

1. `T0 <: TypeVariable(U0)` _from Argument R|<local>/generic|_
2. Combine `T0 <: TypeVariable(U0)` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `T1 <: TypeVariable(U1)`
    2. `TypeVariable(U1) <: T1`
    3. `T2 <: TypeVariable(U2)`
    4. `TypeVariable(U2) <: T2`
    5. `T3 <: TypeVariable(U3)`
    6. `TypeVariable(U3) <: T3`
    7. `T4 <: TypeVariable(U4)`
    8. `TypeVariable(U4) <: T4`
    9. `T5 <: TypeVariable(U5)`
    10. `TypeVariable(U5) <: T5`
    11. `T6 <: TypeVariable(U6)`
    12. `TypeVariable(U6) <: T6`
    13. `T7 <: TypeVariable(U7)`
    14. `TypeVariable(U7) <: T7`
    15. `T8 <: TypeVariable(U8)`
    16. `TypeVariable(U8) <: T8`
    17. `T9 <: TypeVariable(U9)`
    18. `TypeVariable(U9) <: T9`
    19. `Ta <: TypeVariable(Ua)`
    20. `TypeVariable(Ua) <: Ta`
    21. `Tb <: TypeVariable(Ub)`
    22. `TypeVariable(Ub) <: Tb`
    23. `Tc <: TypeVariable(Uc)`
    24. `TypeVariable(Uc) <: Tc`
    25. `Td <: TypeVariable(Ud)`
    26. `TypeVariable(Ud) <: Td`
    27. `Te <: TypeVariable(Ue)`
    28. `TypeVariable(Ue) <: Te`
    29. `Tf <: TypeVariable(Uf)`
    30. `TypeVariable(Uf) <: Tf`
3. Combine `T1 <: TypeVariable(U1)` with `TypeVariable(U1) <: T1`
    1. `TypeVariable(U1) == T1`
4. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
5. Combine `T2 <: TypeVariable(U2)` with `TypeVariable(U2) <: T2`
    1. `TypeVariable(U2) == T2`
6. Combine `TypeVariable(U2) == T2` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<TypeVariable(U1), T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
7. Combine `T3 <: TypeVariable(U3)` with `TypeVariable(U3) <: T3`
    1. `TypeVariable(U3) == T3`
8. Combine `TypeVariable(U3) == T3` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
9. Combine `T4 <: TypeVariable(U4)` with `TypeVariable(U4) <: T4`
    1. `TypeVariable(U4) == T4`
10. Combine `TypeVariable(U4) == T4` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
11. Combine `T5 <: TypeVariable(U5)` with `TypeVariable(U5) <: T5`
    1. `TypeVariable(U5) == T5`
12. Combine `TypeVariable(U5) == T5` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
13. Combine `T6 <: TypeVariable(U6)` with `TypeVariable(U6) <: T6`
    1. `TypeVariable(U6) == T6`
14. Combine `TypeVariable(U6) == T6` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
15. Combine `T7 <: TypeVariable(U7)` with `TypeVariable(U7) <: T7`
    1. `TypeVariable(U7) == T7`
16. Combine `TypeVariable(U7) == T7` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
17. Combine `T8 <: TypeVariable(U8)` with `TypeVariable(U8) <: T8`
    1. `TypeVariable(U8) == T8`
18. Combine `TypeVariable(U8) == T8` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
19. Combine `T9 <: TypeVariable(U9)` with `TypeVariable(U9) <: T9`
    1. `TypeVariable(U9) == T9`
20. Combine `TypeVariable(U9) == T9` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
21. Combine `Ta <: TypeVariable(Ua)` with `TypeVariable(Ua) <: Ta`
    1. `TypeVariable(Ua) == Ta`
22. Combine `TypeVariable(Ua) == Ta` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
23. Combine `Tb <: TypeVariable(Ub)` with `TypeVariable(Ub) <: Tb`
    1. `TypeVariable(Ub) == Tb`
24. Combine `TypeVariable(Ub) == Tb` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
25. Combine `Tc <: TypeVariable(Uc)` with `TypeVariable(Uc) <: Tc`
    1. `TypeVariable(Uc) == Tc`
26. Combine `TypeVariable(Uc) == Tc` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
27. Combine `Td <: TypeVariable(Ud)` with `TypeVariable(Ud) <: Td`
    1. `TypeVariable(Ud) == Td`
28. Combine `TypeVariable(Ud) == Td` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
29. Combine `Te <: TypeVariable(Ue)` with `TypeVariable(Ue) <: Te`
    1. `TypeVariable(Ue) == Te`
30. Combine `TypeVariable(Ue) == Te` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
31. Combine `Tf <: TypeVariable(Uf)` with `TypeVariable(Uf) <: Tf`
    1. `TypeVariable(Uf) == Tf`
32. Combine `TypeVariable(Uf) == Tf` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
33. `T1.(T2) -> T3 <: TypeVariable(U1).(TypeVariable(U2)) -> TypeVariable(U3)` _from Argument R|<local>/first|_
34. `T4.(T5) -> T6 <: TypeVariable(U4).(TypeVariable(U5)) -> TypeVariable(U6)` _from Argument R|<local>/second|_
35. `T7.(T8) -> T9 <: TypeVariable(U7).(TypeVariable(U8)) -> TypeVariable(U9)` _from Argument R|<local>/third|_
36. `Ta.(Tb) -> Tc <: TypeVariable(Ua).(TypeVariable(Ub)) -> TypeVariable(Uc)` _from Argument R|<local>/fourth|_
37. `Td.(Te) -> Tf <: TypeVariable(Ud).(TypeVariable(Ue)) -> TypeVariable(Uf)` _from Argument R|<local>/fifth|_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Any <: kotlin/Any` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(U1)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
    2. `TypeVariable(U2)` is `Readiness(
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
    3. `TypeVariable(U3)` is `Readiness(
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
    4. `TypeVariable(U4)` is `Readiness(
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
    5. `TypeVariable(U5)` is `Readiness(
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
    6. `TypeVariable(U6)` is `Readiness(
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
    7. `TypeVariable(U7)` is `Readiness(
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
    8. `TypeVariable(U8)` is `Readiness(
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
    9. `TypeVariable(U9)` is `Readiness(
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
    10. `TypeVariable(Ua)` is `Readiness(
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
    11. `TypeVariable(Ub)` is `Readiness(
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
    12. `TypeVariable(Uc)` is `Readiness(
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
    13. `TypeVariable(Ud)` is `Readiness(
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
    14. `TypeVariable(Ue)` is `Readiness(
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
    15. `TypeVariable(Uf)` is `Readiness(
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
2. `TypeVariable(U1) == T1` _from Fix variable U1_
3. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
4. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
5. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
6. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
7. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
8. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
9. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
10. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
11. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
12. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
13. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
14. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
15. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
16. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
17. Combine `TypeVariable(U1) == T1` with `TypeVariable(U0) <: Generic<TypeVariable(U1), TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
    1. `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
18. Combine `T0 <: TypeVariable(U0)` with `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `T3 <: TypeVariable(U3)`
    2. `TypeVariable(U3) <: T3`
    3. `T4 <: TypeVariable(U4)`
    4. `TypeVariable(U4) <: T4`
    5. `T5 <: TypeVariable(U5)`
    6. `TypeVariable(U5) <: T5`
    7. `T6 <: TypeVariable(U6)`
    8. `TypeVariable(U6) <: T6`
    9. `T7 <: TypeVariable(U7)`
    10. `TypeVariable(U7) <: T7`
    11. `T8 <: TypeVariable(U8)`
    12. `TypeVariable(U8) <: T8`
    13. `T9 <: TypeVariable(U9)`
    14. `TypeVariable(U9) <: T9`
    15. `Ta <: TypeVariable(Ua)`
    16. `TypeVariable(Ua) <: Ta`
    17. `Tb <: TypeVariable(Ub)`
    18. `TypeVariable(Ub) <: Tb`
    19. `Tc <: TypeVariable(Uc)`
    20. `TypeVariable(Uc) <: Tc`
    21. `Td <: TypeVariable(Ud)`
    22. `TypeVariable(Ud) <: Td`
    23. `Te <: TypeVariable(Ue)`
    24. `TypeVariable(Ue) <: Te`
    25. `Tf <: TypeVariable(Uf)`
    26. `TypeVariable(Uf) <: Tf`
19. Combine `T0 <: TypeVariable(U0)` with `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `T2 <: TypeVariable(U2)`
    2. `TypeVariable(U2) <: T2`
20. Choose `TypeVariable(U2)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
    2. `TypeVariable(U3)` is `Readiness(
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
    3. `TypeVariable(U4)` is `Readiness(
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
    4. `TypeVariable(U5)` is `Readiness(
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
    5. `TypeVariable(U6)` is `Readiness(
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
    6. `TypeVariable(U7)` is `Readiness(
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
    7. `TypeVariable(U8)` is `Readiness(
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
    8. `TypeVariable(U9)` is `Readiness(
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
    9. `TypeVariable(Ua)` is `Readiness(
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
    10. `TypeVariable(Ub)` is `Readiness(
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
    11. `TypeVariable(Uc)` is `Readiness(
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
    12. `TypeVariable(Ud)` is `Readiness(
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
    13. `TypeVariable(Ue)` is `Readiness(
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
    14. `TypeVariable(Uf)` is `Readiness(
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
21. `TypeVariable(U2) == T2` _from Fix variable U2_
22. Combine `TypeVariable(U2) == T2` with `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
23. Combine `TypeVariable(U2) == T2` with `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
24. Combine `TypeVariable(U2) == T2` with `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
25. Combine `TypeVariable(U2) == T2` with `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
26. Combine `TypeVariable(U2) == T2` with `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
27. Combine `TypeVariable(U2) == T2` with `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
28. Combine `TypeVariable(U2) == T2` with `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
29. Combine `TypeVariable(U2) == T2` with `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
30. Combine `TypeVariable(U2) == T2` with `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
31. Combine `TypeVariable(U2) == T2` with `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
32. Combine `TypeVariable(U2) == T2` with `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
33. Combine `TypeVariable(U2) == T2` with `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
34. Combine `TypeVariable(U2) == T2` with `TypeVariable(U0) <: Generic<T1, TypeVariable(U2), TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
    1. `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
35. Choose `TypeVariable(U3)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
    2. `TypeVariable(U4)` is `Readiness(
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
    3. `TypeVariable(U5)` is `Readiness(
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
    4. `TypeVariable(U6)` is `Readiness(
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
    5. `TypeVariable(U7)` is `Readiness(
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
    6. `TypeVariable(U8)` is `Readiness(
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
    7. `TypeVariable(U9)` is `Readiness(
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
    8. `TypeVariable(Ua)` is `Readiness(
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
    9. `TypeVariable(Ub)` is `Readiness(
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
    10. `TypeVariable(Uc)` is `Readiness(
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
    11. `TypeVariable(Ud)` is `Readiness(
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
    12. `TypeVariable(Ue)` is `Readiness(
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
    13. `TypeVariable(Uf)` is `Readiness(
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
36. `TypeVariable(U3) == T3` _from Fix variable U3_
37. Combine `TypeVariable(U3) == T3` with `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
38. Combine `TypeVariable(U3) == T3` with `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
39. Combine `TypeVariable(U3) == T3` with `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
40. Combine `TypeVariable(U3) == T3` with `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
41. Combine `TypeVariable(U3) == T3` with `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
42. Combine `TypeVariable(U3) == T3` with `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
43. Combine `TypeVariable(U3) == T3` with `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
44. Combine `TypeVariable(U3) == T3` with `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
45. Combine `TypeVariable(U3) == T3` with `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
46. Combine `TypeVariable(U3) == T3` with `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
47. Combine `TypeVariable(U3) == T3` with `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
48. Combine `TypeVariable(U3) == T3` with `TypeVariable(U0) <: Generic<T1, T2, TypeVariable(U3), TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
49. Choose `TypeVariable(U4)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
    2. `TypeVariable(U5)` is `Readiness(
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
    3. `TypeVariable(U6)` is `Readiness(
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
    4. `TypeVariable(U7)` is `Readiness(
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
    5. `TypeVariable(U8)` is `Readiness(
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
    6. `TypeVariable(U9)` is `Readiness(
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
    7. `TypeVariable(Ua)` is `Readiness(
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
    8. `TypeVariable(Ub)` is `Readiness(
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
    9. `TypeVariable(Uc)` is `Readiness(
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
    10. `TypeVariable(Ud)` is `Readiness(
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
    11. `TypeVariable(Ue)` is `Readiness(
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
    12. `TypeVariable(Uf)` is `Readiness(
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
50. `TypeVariable(U4) == T4` _from Fix variable U4_
51. Combine `TypeVariable(U4) == T4` with `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
52. Combine `TypeVariable(U4) == T4` with `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
53. Combine `TypeVariable(U4) == T4` with `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
54. Combine `TypeVariable(U4) == T4` with `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
55. Combine `TypeVariable(U4) == T4` with `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
56. Combine `TypeVariable(U4) == T4` with `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
57. Combine `TypeVariable(U4) == T4` with `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
58. Combine `TypeVariable(U4) == T4` with `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
59. Combine `TypeVariable(U4) == T4` with `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
60. Combine `TypeVariable(U4) == T4` with `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
61. Combine `TypeVariable(U4) == T4` with `TypeVariable(U0) <: Generic<T1, T2, T3, TypeVariable(U4), TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
62. Choose `TypeVariable(U5)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
    2. `TypeVariable(U6)` is `Readiness(
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
    3. `TypeVariable(U7)` is `Readiness(
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
    4. `TypeVariable(U8)` is `Readiness(
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
    5. `TypeVariable(U9)` is `Readiness(
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
    6. `TypeVariable(Ua)` is `Readiness(
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
    7. `TypeVariable(Ub)` is `Readiness(
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
    8. `TypeVariable(Uc)` is `Readiness(
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
    9. `TypeVariable(Ud)` is `Readiness(
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
    10. `TypeVariable(Ue)` is `Readiness(
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
    11. `TypeVariable(Uf)` is `Readiness(
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
63. `TypeVariable(U5) == T5` _from Fix variable U5_
64. Combine `TypeVariable(U5) == T5` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
65. Combine `TypeVariable(U5) == T5` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
66. Combine `TypeVariable(U5) == T5` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
67. Combine `TypeVariable(U5) == T5` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
68. Combine `TypeVariable(U5) == T5` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
69. Combine `TypeVariable(U5) == T5` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
70. Combine `TypeVariable(U5) == T5` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
71. Combine `TypeVariable(U5) == T5` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
72. Combine `TypeVariable(U5) == T5` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
73. Combine `TypeVariable(U5) == T5` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, TypeVariable(U5), TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
74. Choose `TypeVariable(U6)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
    2. `TypeVariable(U7)` is `Readiness(
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
    3. `TypeVariable(U8)` is `Readiness(
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
    4. `TypeVariable(U9)` is `Readiness(
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
    5. `TypeVariable(Ua)` is `Readiness(
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
    6. `TypeVariable(Ub)` is `Readiness(
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
    7. `TypeVariable(Uc)` is `Readiness(
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
    8. `TypeVariable(Ud)` is `Readiness(
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
    9. `TypeVariable(Ue)` is `Readiness(
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
    10. `TypeVariable(Uf)` is `Readiness(
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
75. `TypeVariable(U6) == T6` _from Fix variable U6_
76. Combine `TypeVariable(U6) == T6` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
77. Combine `TypeVariable(U6) == T6` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
78. Combine `TypeVariable(U6) == T6` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
79. Combine `TypeVariable(U6) == T6` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
80. Combine `TypeVariable(U6) == T6` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
81. Combine `TypeVariable(U6) == T6` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
82. Combine `TypeVariable(U6) == T6` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
83. Combine `TypeVariable(U6) == T6` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
84. Combine `TypeVariable(U6) == T6` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, TypeVariable(U6), TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
85. Choose `TypeVariable(U7)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
    2. `TypeVariable(U8)` is `Readiness(
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
    3. `TypeVariable(U9)` is `Readiness(
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
    4. `TypeVariable(Ua)` is `Readiness(
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
    5. `TypeVariable(Ub)` is `Readiness(
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
    6. `TypeVariable(Uc)` is `Readiness(
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
    7. `TypeVariable(Ud)` is `Readiness(
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
    8. `TypeVariable(Ue)` is `Readiness(
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
    9. `TypeVariable(Uf)` is `Readiness(
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
86. `TypeVariable(U7) == T7` _from Fix variable U7_
87. Combine `TypeVariable(U7) == T7` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
88. Combine `TypeVariable(U7) == T7` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
89. Combine `TypeVariable(U7) == T7` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
90. Combine `TypeVariable(U7) == T7` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
91. Combine `TypeVariable(U7) == T7` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
92. Combine `TypeVariable(U7) == T7` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
93. Combine `TypeVariable(U7) == T7` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
94. Combine `TypeVariable(U7) == T7` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, TypeVariable(U7), TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
95. Choose `TypeVariable(U8)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
    2. `TypeVariable(U9)` is `Readiness(
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
    3. `TypeVariable(Ua)` is `Readiness(
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
    4. `TypeVariable(Ub)` is `Readiness(
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
    5. `TypeVariable(Uc)` is `Readiness(
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
    6. `TypeVariable(Ud)` is `Readiness(
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
    7. `TypeVariable(Ue)` is `Readiness(
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
    8. `TypeVariable(Uf)` is `Readiness(
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
96. `TypeVariable(U8) == T8` _from Fix variable U8_
97. Combine `TypeVariable(U8) == T8` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
98. Combine `TypeVariable(U8) == T8` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
99. Combine `TypeVariable(U8) == T8` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
100. Combine `TypeVariable(U8) == T8` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
101. Combine `TypeVariable(U8) == T8` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
102. Combine `TypeVariable(U8) == T8` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
103. Combine `TypeVariable(U8) == T8` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, TypeVariable(U8), TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
104. Choose `TypeVariable(U9)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
    2. `TypeVariable(Ua)` is `Readiness(
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
    3. `TypeVariable(Ub)` is `Readiness(
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
    4. `TypeVariable(Uc)` is `Readiness(
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
    5. `TypeVariable(Ud)` is `Readiness(
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
    6. `TypeVariable(Ue)` is `Readiness(
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
    7. `TypeVariable(Uf)` is `Readiness(
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
105. `TypeVariable(U9) == T9` _from Fix variable U9_
106. Combine `TypeVariable(U9) == T9` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, TypeVariable(U9), Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
107. Combine `TypeVariable(U9) == T9` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, TypeVariable(U9), TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
108. Combine `TypeVariable(U9) == T9` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
109. Combine `TypeVariable(U9) == T9` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
110. Combine `TypeVariable(U9) == T9` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
111. Combine `TypeVariable(U9) == T9` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, TypeVariable(U9), TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
112. Choose `TypeVariable(Ua)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
    2. `TypeVariable(Ub)` is `Readiness(
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
    3. `TypeVariable(Uc)` is `Readiness(
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
    4. `TypeVariable(Ud)` is `Readiness(
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
    5. `TypeVariable(Ue)` is `Readiness(
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
    6. `TypeVariable(Uf)` is `Readiness(
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
113. `TypeVariable(Ua) == Ta` _from Fix variable Ua_
114. Combine `TypeVariable(Ua) == Ta` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, TypeVariable(Ua), Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
115. Combine `TypeVariable(Ua) == Ta` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, TypeVariable(Ua), TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
116. Combine `TypeVariable(Ua) == Ta` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
117. Combine `TypeVariable(Ua) == Ta` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
118. Combine `TypeVariable(Ua) == Ta` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, TypeVariable(Ua), TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
119. Choose `TypeVariable(Ub)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
    2. `TypeVariable(Uc)` is `Readiness(
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
    3. `TypeVariable(Ud)` is `Readiness(
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
    4. `TypeVariable(Ue)` is `Readiness(
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
    5. `TypeVariable(Uf)` is `Readiness(
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
120. `TypeVariable(Ub) == Tb` _from Fix variable Ub_
121. Combine `TypeVariable(Ub) == Tb` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, TypeVariable(Ub), Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, Tc, TypeVariable(Ud), TypeVariable(Ue), TypeVariable(Uf)>`
122. Combine `TypeVariable(Ub) == Tb` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, TypeVariable(Ub), TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
123. Combine `TypeVariable(Ub) == Tb` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
124. Combine `TypeVariable(Ub) == Tb` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, TypeVariable(Ub), TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
125. Choose `TypeVariable(Uc)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
    2. `TypeVariable(Ud)` is `Readiness(
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
    3. `TypeVariable(Ue)` is `Readiness(
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
    4. `TypeVariable(Uf)` is `Readiness(
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
126. `TypeVariable(Uc) == Tc` _from Fix variable Uc_
127. Combine `TypeVariable(Uc) == Tc` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, TypeVariable(Uc), Td, TypeVariable(Ue), TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, Tc, Td, TypeVariable(Ue), TypeVariable(Uf)>`
128. Combine `TypeVariable(Uc) == Tc` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, TypeVariable(Uc), TypeVariable(Ud), Te, TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, Tc, TypeVariable(Ud), Te, TypeVariable(Uf)>`
129. Combine `TypeVariable(Uc) == Tc` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, TypeVariable(Uc), TypeVariable(Ud), TypeVariable(Ue), Tf>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, Tc, TypeVariable(Ud), TypeVariable(Ue), Tf>`
130. Choose `TypeVariable(Ud)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
    2. `TypeVariable(Ue)` is `Readiness(
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
    3. `TypeVariable(Uf)` is `Readiness(
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
131. `TypeVariable(Ud) == Td` _from Fix variable Ud_
132. Combine `TypeVariable(Ud) == Td` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, Tc, TypeVariable(Ud), Te, TypeVariable(Uf)>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, Tc, Td, Te, TypeVariable(Uf)>`
133. Combine `TypeVariable(Ud) == Td` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, Tc, TypeVariable(Ud), TypeVariable(Ue), Tf>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, Tc, Td, TypeVariable(Ue), Tf>`
134. Choose `TypeVariable(Ue)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
    2. `TypeVariable(Uf)` is `Readiness(
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
135. `TypeVariable(Ue) == Te` _from Fix variable Ue_
136. Combine `TypeVariable(Ue) == Te` with `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, Tc, Td, TypeVariable(Ue), Tf>`
    1. `TypeVariable(U0) <: Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, Tc, Td, Te, Tf>`
137. Choose `TypeVariable(Uf)` with `Readiness(
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
    1. `TypeVariable(U0)` is `Readiness(
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
138. `TypeVariable(Uf) == Tf` _from Fix variable Uf_
139. Choose `TypeVariable(U0)` with `Readiness(
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
140. `TypeVariable(U0) == T0` _from Fix variable U0_

### Call 2

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing↩`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: kotlin/Any` _from ExpectedType for some call_
