// RUN_PIPELINE_TILL: BACKEND
class Generic<T>

fun redundantNullable(
        i: Int?<!REDUNDANT_NULLABLE!>?<!>,
        three: Int?<!REDUNDANT_NULLABLE!>?<!><!REDUNDANT_NULLABLE!>?<!>,
        gOut: Generic<Int>?<!REDUNDANT_NULLABLE!>?<!>,
        gIn: Generic<Int?<!REDUNDANT_NULLABLE!>?<!>>
) {
}

private typealias AA = String?

fun m(m: IFoo<AA>) {
        m.onProcessed {}
}

interface IFoo<T> {
        fun onProcessed(f: (T?) -> Unit)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, typeParameter */
