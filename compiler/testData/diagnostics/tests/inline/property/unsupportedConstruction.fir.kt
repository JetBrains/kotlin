// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline val z: Int
    get()  {

    <!NOT_YET_SUPPORTED_IN_INLINE!>class<!> A {
        <!NOT_YET_SUPPORTED_IN_INLINE!>fun<!> a() {
           <!NOT_YET_SUPPORTED_IN_INLINE!>class<!> AInner {}
        }
    }

    <!LOCAL_OBJECT_NOT_ALLOWED!>object B<!>{
        <!LOCAL_OBJECT_NOT_ALLOWED!>object BInner<!> {}
    }

    <!NOT_YET_SUPPORTED_IN_INLINE!>fun<!> local() {
        <!NOT_YET_SUPPORTED_IN_INLINE!>fun<!> localInner() {}
    }
    return 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, localClass, localFunction,
nestedClass, objectDeclaration, propertyDeclaration */
