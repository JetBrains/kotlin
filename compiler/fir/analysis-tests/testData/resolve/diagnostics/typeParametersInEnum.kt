// RUN_PIPELINE_TILL: FRONTEND
enum class A<<!TYPE_PARAMETERS_IN_ENUM!>B<!>, C : B, D>

enum class B

/* GENERATED_FIR_TAGS: enumDeclaration, nullableType, typeConstraint, typeParameter */
