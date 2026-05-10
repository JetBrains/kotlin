// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +EnhancementsOfSecondIncorporationKind25
// ISSUE: KT-83068
// DUMP_INFERENCE_LOGS: FIXATION, MARKDOWN

interface Generic<T1 : Alpha, T2 : Beta, T3 : Gamma, T4 : Delta, T5 : Epsilon, T6 : Psi, T7 : Omega, T8 : Sigma, T9 : Rho, Ta : Mu, Tb : Nu, Tc : Pi, Td : Dzeta, Te : Teta, Tf : Jot>

interface Alpha
interface Beta
interface Gamma
interface Delta
interface Epsilon
interface Psi
interface Omega
interface Sigma
interface Rho
interface Mu
interface Nu
interface Pi
interface Dzeta
interface Teta
interface Jot

fun <T0 : Generic<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ta, Tb, Tc, Td, Te, Tf>,
     T1 : Alpha, T2 : Beta, T3 : Gamma, T4 : Delta, T5 : Epsilon, T6 : Psi, T7 : Omega, T8 : Sigma, T9 : Rho, Ta : Mu, Tb : Nu, Tc : Pi, Td : Dzeta, Te : Teta, Tf : Jot> foo(
    generic: T0,
    first: T1.(T2) -> T3,
    second: T4.(T5) -> T6,
    third: T7.(T8) -> T9,
    fourth: Ta.(Tb) -> Tc,
    fifth: Td.(Te) -> Tf,
): Any = bar(generic, first, second, third, fourth, fifth)

fun <U0 : Generic<U1, U2, U3, U4, U5, U6, U7, U8, U9, Ua, Ub, Uc, Ud, Ue, Uf>,
     U1 : Alpha, U2 : Beta, U3 : Gamma, U4 : Delta, U5 : Epsilon, U6 : Psi, U7 : Omega, U8 : Sigma, U9 : Rho, Ua : Mu, Ub : Nu, Uc : Pi, Ud : Dzeta, Ue : Teta, Uf : Jot> bar(
    generic: U0,
    first: U1.(U2) -> U3,
    second: U4.(U5) -> U6,
    third: U7.(U8) -> U9,
    fourth: Ua.(Ub) -> Uc,
    fifth: Ud.(Ue) -> Uf,
): Any = TODO()

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, interfaceDeclaration, typeConstraint, typeParameter,
typeWithExtension */
