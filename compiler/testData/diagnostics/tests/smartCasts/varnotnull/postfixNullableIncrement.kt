// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
operator fun Int?.inc(): Int? { return this }

public fun box(arg: Int?) : Int? {
    var i : Int? = arg
    var j = i++
    j<!UNSAFE_CALL!>.<!>toInt()
    return i
}

/* GENERATED_FIR_TAGS: assignment, funWithExtensionReceiver, functionDeclaration, incrementDecrementExpression,
localProperty, nullableType, operator, propertyDeclaration, thisExpression */
