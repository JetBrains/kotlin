// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
interface A

interface B {
    val b: Boolean
}

val A.check_1: Boolean
    get() = this is B && b

val A.check_2: Boolean
    get() = this is B && this.b

/* GENERATED_FIR_TAGS: andExpression, getter, interfaceDeclaration, intersectionType, isExpression, propertyDeclaration,
propertyWithExtensionReceiver, smartcast, thisExpression */
