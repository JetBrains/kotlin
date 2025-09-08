// RUN_PIPELINE_TILL: FRONTEND
public fun test() {

    fun localFun() {

    }

    <!NOT_YET_SUPPORTED_IN_INLINE!>inline<!> fun localFun2() {
        localFun()
    }

}

/* GENERATED_FIR_TAGS: functionDeclaration, inline, localFunction */
