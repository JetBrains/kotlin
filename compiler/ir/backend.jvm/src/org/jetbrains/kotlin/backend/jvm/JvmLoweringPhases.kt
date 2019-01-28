/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.CompilerPhase
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.makePhase
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.lower.*
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.name.NameUtils

object IrFileStartPhase : CompilerPhase<BackendContext, IrFile> {
    override val name = "IrFileStart"
    override val description = "State at start of IrFile lowering"
    override val prerequisite = emptySet()
    override fun invoke(context: BackendContext, input: IrFile) = input
}

private fun makeJvmPhase(
    lowering: (JvmBackendContext, IrFile) -> Unit,
    description: String,
    name: String,
    prerequisite: Set<CompilerPhase<JvmBackendContext, IrFile>> = emptySet()
) = makePhase(lowering, description, name, prerequisite)

private val JvmCoercionToUnitPhase = makeJvmPhase(
    { context, file -> JvmCoercionToUnitPatcher(context).lower(file) },
    name = "JvmCoercionToUnit",
    description = "Insert conversions to unit after IrCalls where needed"
)

private val FileClassPhase = makeJvmPhase(
    { context, file -> FileClassLowering(context).lower(file) },
    name = "FileClass",
    description = "Put file level function and property declaration into a class"
)

private val KCallableNamePropertyPhase = makeJvmPhase(
    { context, file -> KCallableNamePropertyLowering(context).lower(file) },
    name = "KCallableNameProperty",
    description = "Replace name references for callables with constants"
)

private val LateinitPhase = makeJvmPhase(
    { context, file -> LateinitLowering(context).lower(file) },
    name = "Lateinit",
    description = "Insert checks for lateinit field references"
)

private val MoveCompanionObjectFieldsPhase = makeJvmPhase(
    { context, file -> MoveCompanionObjectFieldsLowering(context).runOnFilePostfix(file) },
    name = "MoveCompanionObjectFields",
    description = "Move companion object fields to static fields of companion's owner"
)


private val ConstAndJvmFieldPropertiesPhase = makeJvmPhase(
    { context, file -> ConstAndJvmFieldPropertiesLowering(context).lower(file) },
    name = "ConstAndJvmFieldProperties",
    description = "Substitute calls to const and Jvm>Field properties with const/field access"
)


private val PropertiesPhase = makeJvmPhase(
    { context, file -> PropertiesLowering(context).lower(file) },
    name = "Properties",
    description = "move fields and accessors for properties to their classes"
)


private val AnnotationPhase = makeJvmPhase(
    { _, file -> AnnotationLowering().lower(file) },
    name = "Annotation",
    description = "Remove constructors from annotation classes"
)

private val DefaultArgumentStubPhase = makeJvmPhase(
    { context, file -> DefaultArgumentStubGenerator(context, false).lower(file) },
    name = "DefaultArgumentsStubGenerator",
    description = "Generate synthetic stubs for functions with default parameter values"
)

private val InterfacePhase = makeJvmPhase(
    { context, file -> InterfaceLowering(context).lower(file) },
    name = "Interface",
    description = "Move default implementations of interface members to DefaultImpls class"
)

private val InterfaceDelegationPhase = makeJvmPhase(
    { context, file -> InterfaceDelegationLowering(context).lower(file) },
    name = "InterfaceDelegation",
    description = "Delegate calls to interface members with default implementations to DefaultImpls"
)

private val SharedVariablesPhase = makeJvmPhase(
    { context, file -> SharedVariablesLowering(context).lower(file) },
    name = "SharedVariables",
    description = "Transform shared variables"
)

private val LocalDeclarationsPhase = makeJvmPhase(
    { context, data ->
        LocalDeclarationsLowering(context, object : LocalNameProvider {
            override fun localName(declaration: IrDeclarationWithName): String =
                NameUtils.sanitizeAsJavaIdentifier(super.localName(declaration))
        }, Visibilities.PUBLIC, true).lower(data)
    },
    name = "JvmLocalDeclarations",
    description = "Move local declarations to classes",
    prerequisite = setOf(SharedVariablesPhase)
)

private val CallableReferencePhase = makeJvmPhase(
    { context, file -> CallableReferenceLowering(context).lower(file) },
    name = "CallableReference",
    description = "Handle callable references"
)

private val FunctionNVarargInvokePhase = makeJvmPhase(
    { context, file -> FunctionNVarargInvokeLowering(context).lower(file) },
    name = "FunctionNVarargInvoke",
    description = "Handle invoke functions with large number of arguments"
)


private val InnerClassesPhase = makeJvmPhase(
    { context, file -> InnerClassesLowering(context).lower(file) },
    name = "InnerClasses",
    description = "Move inner classes to toplevel"
)

private val InnerClassConstructorCallsPhase = makeJvmPhase(
    { context, file -> InnerClassConstructorCallsLowering(context).lower(file) },
    name = "InnerClassConstructorCalls",
    description = "Handle constructor calls for inner classes"
)


private val EnumClassPhase = makeJvmPhase(
    { context, file -> EnumClassLowering(context).lower(file) },
    name = "EnumClass",
    description = "Handle enum classes"
)


private val ObjectClassPhase = makeJvmPhase(
    { context, file -> ObjectClassLowering(context).lower(file) },
    name = "ObjectClass",
    description = "Handle object classes"
)

private val InitializersPhase = makeJvmPhase(
    { context, file -> InitializersLowering(context, JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER, true).lower(file) },
    name = "Initializers",
    description = "Handle initializer statements"
)


private val SingletonReferencesPhase = makeJvmPhase(
    { context, file -> SingletonReferencesLowering(context).lower(file) },
    name = "SingletonReferences",
    description = "Handle singleton references"
)


private val SyntheticAccessorPhase = makeJvmPhase(
    { context, file -> SyntheticAccessorLowering(context).lower(file) },
    name = "SyntheticAccessor",
    description = "Introduce synthetic accessors",
    prerequisite = setOf(ObjectClassPhase)
)

private val BridgePhase = makeJvmPhase(
    { context, file -> BridgeLowering(context).lower(file) },
    name = "Bridge",
    description = "Generate bridges"
)


private val JvmOverloadsAnnotationPhase = makeJvmPhase(
    { context, file -> JvmOverloadsAnnotationLowering(context).lower(file) },
    name = "JvmOverloadsAnnotation",
    description = "Handle JvmOverloads annotations"
)


private val JvmStaticAnnotationPhase = makeJvmPhase(
    { context, file -> JvmStaticAnnotationLowering(context).lower(file) },
    name = "JvmStaticAnnotation",
    description = "Handle JvmStatic annotations"
)


private val StaticDefaultFunctionPhase = makeJvmPhase(
    { _, file -> StaticDefaultFunctionLowering().lower(file) },
    name = "StaticDefaultFunction",
    description = "Generate static functions for default parameters"
)


private val TailrecPhase = makeJvmPhase(
    { context, file -> TailrecLowering(context).lower(file) },
    name = "Tailrec",
    description = "Handle tailrec calls"
)

private val ToArrayPhase = makeJvmPhase(
    { context, file -> ToArrayLowering(context).lower(file) },
    name = "ToArray",
    description = "Handle toArray functions"
)

private val JvmBuiltinOptimizationLowering = makeJvmPhase(
    { context, file -> JvmBuiltinOptimizationLowering(context).lower(file) },
    name = "JvmBuiltinOptimizationLowering",
    description = "Optimize builtin calls for JVM code generation"
)

object IrFileEndPhase : CompilerPhase<BackendContext, IrFile> {
    override val name = "IrFileEnd"
    override val description = "State at end of IrFile lowering"
    override val prerequisite = emptySet()
    override fun invoke(context: BackendContext, input: IrFile) = input
}

val jvmPhases = listOf(
    IrFileStartPhase,

    JvmCoercionToUnitPhase,
    FileClassPhase,
    KCallableNamePropertyPhase,

    LateinitPhase,

    MoveCompanionObjectFieldsPhase,
    ConstAndJvmFieldPropertiesPhase,
    PropertiesPhase,
    AnnotationPhase,

    DefaultArgumentStubPhase,

    InterfacePhase,
    InterfaceDelegationPhase,
    SharedVariablesPhase,

    makePatchParentsPhase(1),

    LocalDeclarationsPhase,
    CallableReferencePhase,
    FunctionNVarargInvokePhase,

    InnerClassesPhase,
    InnerClassConstructorCallsPhase,

    makePatchParentsPhase(2),

    EnumClassPhase,
    ObjectClassPhase,
    InitializersPhase,
    SingletonReferencesPhase,
    SyntheticAccessorPhase,
    BridgePhase,
    JvmOverloadsAnnotationPhase,
    JvmStaticAnnotationPhase,
    StaticDefaultFunctionPhase,

    TailrecPhase,
    ToArrayPhase,
    JvmBuiltinOptimizationLowering,

    makePatchParentsPhase(3),

    IrFileEndPhase
)
