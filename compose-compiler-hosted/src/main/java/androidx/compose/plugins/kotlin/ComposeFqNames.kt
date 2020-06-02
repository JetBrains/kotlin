/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE
import org.jetbrains.kotlin.types.TypeUtils.UNIT_EXPECTED_TYPE
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

object ComposeFqNames {
    val Composable = ComposeUtils.composeFqName("Composable")
    val CurrentComposerIntrinsic = ComposeUtils.composeFqName("<get-currentComposer>")
    val ComposableContract = ComposeUtils.composeFqName("ComposableContract")
    val restartableFunction = FqName.fromSegments(listOf(
        "androidx",
        "compose",
        "internal",
        "restartableFunction"
    ))
    val remember = ComposeUtils.composeFqName("remember")
    val key = ComposeUtils.composeFqName("key")
    val StableMarker = ComposeUtils.composeFqName("StableMarker")
    val Stable = ComposeUtils.composeFqName("Stable")
    val Composer = ComposeUtils.composeFqName("Composer")
    val Untracked = ComposeUtils.composeFqName("Untracked")
    val UiComposer = FqName.fromSegments(listOf("androidx", "ui", "node", "UiComposer"))
    val Package = FqName.fromSegments(listOf("androidx", "compose"))
    val Function0 = FqName.fromSegments(listOf("kotlin", "jvm", "functions", "Function0"))
    val Function1 = FqName.fromSegments(listOf("kotlin", "jvm", "functions", "Function1"))
    fun makeComposableAnnotation(module: ModuleDescriptor): AnnotationDescriptor =
        object : AnnotationDescriptor {
            override val type: KotlinType
                get() = module.findClassAcrossModuleDependencies(
                    ClassId.topLevel(Composable)
                )!!.defaultType
            override val allValueArguments: Map<Name, ConstantValue<*>> get() = emptyMap()
            override val source: SourceElement get() = SourceElement.NO_SOURCE
            override fun toString() = "[@Composable]"
        }
}

fun KotlinType.makeComposable(module: ModuleDescriptor): KotlinType {
    if (hasComposableAnnotation()) return this
    val annotation = ComposeFqNames.makeComposableAnnotation(module)
    return replaceAnnotations(Annotations.create(annotations + annotation))
}

fun KotlinType.hasComposableAnnotation(): Boolean =
    !isSpecialType && annotations.findAnnotation(ComposeFqNames.Composable) != null
fun KotlinType.isMarkedStable(): Boolean =
    !isSpecialType && (
                    annotations.hasStableMarker() ||
                    (constructor.declarationDescriptor?.annotations?.hasStableMarker() ?: false))
fun Annotated.hasComposableAnnotation(): Boolean =
    annotations.findAnnotation(ComposeFqNames.Composable) != null
fun Annotated.composableRestartableContract(): Boolean? {
    val contract = annotations.findAnnotation(ComposeFqNames.ComposableContract) ?: return null
    return contract.argumentValue("restartable")?.value as? Boolean
}
fun Annotated.composableReadonlyContract(): Boolean? {
    val contract = annotations.findAnnotation(ComposeFqNames.ComposableContract) ?: return null
    return contract.argumentValue("readonly")?.value as? Boolean
}
fun Annotated.hasUntrackedAnnotation(): Boolean =
    annotations.findAnnotation(ComposeFqNames.Untracked) != null

internal val KotlinType.isSpecialType: Boolean get() =
    this === NO_EXPECTED_TYPE || this === UNIT_EXPECTED_TYPE

val AnnotationDescriptor.isComposableAnnotation: Boolean get() = fqName == ComposeFqNames.Composable

fun Annotations.hasStableMarker(): Boolean = any(AnnotationDescriptor::isStableMarker)

fun AnnotationDescriptor.isStableMarker(): Boolean {
    val classDescriptor = annotationClass ?: return false
    return classDescriptor.annotations.hasAnnotation(ComposeFqNames.StableMarker)
}