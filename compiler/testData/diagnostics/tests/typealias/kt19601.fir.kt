// RUN_PIPELINE_TILL: FRONTEND
interface Order<T>

typealias Ord<T> = Order<T>

class Test1<T1 : Ord<T1>>

interface Num<T : Number>

typealias N<T> = Num<T>

class Test2<T : N<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>String<!>>>

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, nullableType, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
