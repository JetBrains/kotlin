// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-71662
fun reproduce() {
    val x = pcla { otvOwner ->
        otvOwner.constrain(GenericKlass(TypeArgument))
        otvOwner.provide().fix()
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("GenericKlass<TypeArgument>")!>x<!>
}

class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

class GenericKlass<GKT>(arg: GKT)
object TypeArgument

fun <PH> PH.fix() {}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
lambdaLiteral, localProperty, nullableType, objectDeclaration, primaryConstructor, propertyDeclaration, typeParameter */
