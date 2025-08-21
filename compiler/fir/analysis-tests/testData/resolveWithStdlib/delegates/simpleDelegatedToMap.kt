// RUN_PIPELINE_TILL: BACKEND
class C(val map: MutableMap<String, Any>) {
    var foo by map
}

var bar by hashMapOf<String, Any>()

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, propertyDelegate, setter */
