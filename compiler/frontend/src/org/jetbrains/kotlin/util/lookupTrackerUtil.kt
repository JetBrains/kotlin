package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.getScopeKind
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit

fun LookupTracker.record(expression: KtExpression, type: KotlinType) {
    if (type.isError || type.isUnit()) return

    val typeDescriptor = type.constructor.declarationDescriptor ?: return
    val scopeDescriptor = typeDescriptor.containingDeclaration

    // Scope descriptor is function descriptor only when type is local
    // Lookups for local types are not needed since all usages are compiled with the type
    if (getScopeKind(scopeDescriptor) != null && !DescriptorUtils.isLocal(typeDescriptor)) {
        record(KotlinLookupLocation(expression), scopeDescriptor, typeDescriptor.name)
    }

    for (typeArgument in type.arguments) {
        if (!typeArgument.isStarProjection) {
            record(expression, typeArgument.type)
        }
    }
}

