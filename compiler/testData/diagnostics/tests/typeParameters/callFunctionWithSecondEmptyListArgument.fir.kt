// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// DUMP_INFERENCE_LOGS: FIXATION

interface CallableMemberDescriptor
interface SimpleFunctionDescriptor : CallableMemberDescriptor

public fun <D : CallableMemberDescriptor> resolveOverridesForNonStaticMembers(
    membersFromSupertypes: Collection<D>,
    membersFromCurrent: Collection<D>,
): Collection<D> = membersFromSupertypes

fun computeNonDeclaredFunctions(functionsFromSupertypes: Set<SimpleFunctionDescriptor>) {
    val mergedFunctionFromSuperTypes = resolveOverridesForNonStaticMembers(functionsFromSupertypes, emptyList())

    addOverriddenSpecialMethods(<!ARGUMENT_TYPE_MISMATCH!>mergedFunctionFromSuperTypes<!>)
}

fun addOverriddenSpecialMethods(alreadyDeclaredFunctions: Collection<SimpleFunctionDescriptor>) {}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, inProjection,
intersectionType, nullableType, primaryConstructor, propertyDeclaration, thisExpression, typeConstraint, typeParameter */
