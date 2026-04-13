// RUN_PIPELINE_TILL: BACKEND
class RedefinePropertyInFunction() {

    var i = 17
    
    fun f(): Int {
        var i = 18
        return i
    }

}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, primaryConstructor,
propertyDeclaration */
