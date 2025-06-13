// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProhibitDataClassesOverridingCopy

interface WithCopy<T> {
    fun copy(str: T): WithCopy<T>
}

<!DATA_CLASS_OVERRIDE_DEFAULT_VALUES!>data<!> class Test(val str: String): WithCopy<String>

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, interfaceDeclaration, nullableType,
primaryConstructor, propertyDeclaration, typeParameter */
