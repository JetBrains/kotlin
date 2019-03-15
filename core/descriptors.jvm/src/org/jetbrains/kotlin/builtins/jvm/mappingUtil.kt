/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.jvm

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

fun createMappedTypeParametersSubstitution(from: ClassDescriptor, to: ClassDescriptor): TypeConstructorSubstitution {
    assert(from.declaredTypeParameters.size == to.declaredTypeParameters.size) {
        "$from and $to should have same number of type parameters, " +
                "but ${from.declaredTypeParameters.size} / ${to.declaredTypeParameters.size} found"
    }

    return TypeConstructorSubstitution.createByConstructorsMap(
        from.declaredTypeParameters.map(TypeParameterDescriptor::getTypeConstructor).zip(
            to.declaredTypeParameters.map { it.defaultType.asTypeProjection() }
        ).toMap())
}
