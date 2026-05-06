// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-53267

// KT-53267: Nested typealias call with an upperbound doesn't typecheck correctly

interface ApiTypeSupertype

class ApiType private constructor() : ApiTypeSupertype

// The code compiles if the `ApiTypeSupertype` upper bound is removed.
class ApiTakingType<ApiType : ApiTypeSupertype, RequestData>

typealias Api<RequestData> = ApiTakingType<ApiType, RequestData>

// `SimpleApi` is actually the same type as `Api`.
typealias SimpleApi<RequestData> = Api<RequestData>

// This should work (same as api2 below).
val api = SimpleApi<Unit>()

// This works.
val api2 = Api<Unit>()

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, nullableType, primaryConstructor, propertyDeclaration,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
