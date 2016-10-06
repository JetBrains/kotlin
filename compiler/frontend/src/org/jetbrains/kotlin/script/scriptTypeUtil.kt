/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.script

import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.deserialization.NotFoundClasses
import org.jetbrains.kotlin.serialization.deserialization.findNonGenericClassAcrossDependencies
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.*
import kotlin.reflect.*

data class ScriptParameter(val name: Name, val type: KotlinType)

fun KotlinScriptDefinition.getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter> =
        template.primaryConstructor?.parameters
                ?.map { ScriptParameter(Name.identifier(it.name!!), getKotlinTypeByKType(scriptDescriptor, it.type)) }
        ?: emptyList()

fun getKotlinType(scriptDescriptor: ScriptDescriptor, kClass: KClass<out Any>): KotlinType =
        getKotlinTypeByFqName(scriptDescriptor,
                              kClass.qualifiedName ?: throw RuntimeException("Cannot get FQN from $kClass"))

fun getKotlinTypeByFqName(scriptDescriptor: ScriptDescriptor, fqName: String): KotlinType =
        scriptDescriptor.module.findNonGenericClassAcrossDependencies(
                ClassId.topLevel(FqName(fqName)),
                NotFoundClasses(LockBasedStorageManager.NO_LOCKS, scriptDescriptor.module)
        ).defaultType

// TODO: support star projections
// TODO: support annotations on types and type parameters
// TODO: support type parameters on types and type projections
fun getKotlinTypeByKType(scriptDescriptor: ScriptDescriptor, kType: KType): KotlinType {
    val classifier = kType.classifier
    if (classifier !is KClass<*>)
        throw java.lang.UnsupportedOperationException("Only classes are supported as parameters in script template: $classifier")

    val type = getKotlinType(scriptDescriptor, classifier)
    val typeProjections = kType.arguments.map { getTypeProjection(scriptDescriptor, it) }
    val isNullable = kType.isMarkedNullable

    return KotlinTypeFactory.simpleType(Annotations.EMPTY, type.constructor, typeProjections, isNullable)
}

private fun getTypeProjection(scriptDescriptor: ScriptDescriptor, kTypeProjection: KTypeProjection): TypeProjection {
    val kType = kTypeProjection.type ?: throw java.lang.UnsupportedOperationException("Star projections are not supported")

    val type = getKotlinTypeByKType(scriptDescriptor, kType)

    val variance = when (kTypeProjection.variance) {
        KVariance.IN -> Variance.IN_VARIANCE
        KVariance.OUT -> Variance.OUT_VARIANCE
        KVariance.INVARIANT -> Variance.INVARIANT
        null -> throw java.lang.UnsupportedOperationException("Star projections are not supported")
    }

    return TypeProjectionImpl(variance, type)
}