/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.lower.*
import org.jetbrains.kotlin.backend.jvm.JvmLoweringPhase.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.PatchDeclarationParentsVisitor
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.NameUtils

class JvmLower(val context: JvmBackendContext) {
    fun lower(irFile: IrFile) {
        // TODO run lowering passes as callbacks in bottom-up visitor
        context.rootPhaseManager(irFile).apply {

            phase(START_LOWERING) {}

            phase(COERCION_TO_UNIT_PATCHER) {
                JvmCoercionToUnitPatcher(
                    context.builtIns,
                    context.irBuiltIns,
                    TypeTranslator(context.ir.symbols.externalSymbolTable, context.state.languageVersionSettings)
                ).lower(irFile)
            }
            phase(FILE_CLASS) { FileClassLowering(context).lower(irFile) }
            phase(KCALLABLE_NAME_PROPERTY) { KCallableNamePropertyLowering(context).lower(irFile) }

            phase(LATEINIT) { LateinitLowering(context, true).lower(irFile) }

            MoveCompanionObjectFieldsLowering(context).runOnFilePostfix(irFile)
            phase(CONST_AND_JVM_PROPERTIES) { ConstAndJvmFieldPropertiesLowering(context).lower(irFile) }
            phase(PROPERTIES) { PropertiesLowering().lower(irFile) }
            phase(ANNOTATION) { AnnotationLowering().runOnFilePostfix(irFile) } //should be run before defaults lowering

            //Should be before interface lowering
            phase(DEFAULT_ARGUMENT_STUB_GENERATOR) { DefaultArgumentStubGenerator(context, false).runOnFilePostfix(irFile) }

            phase(INTERFACE) { InterfaceLowering(context).runOnFilePostfix(irFile) }
            phase(INTERFACE_DELEGATION) { InterfaceDelegationLowering(context).runOnFilePostfix(irFile) }
            phase(SHARED_VARIABLES) { SharedVariablesLowering(context).runOnFilePostfix(irFile) }

            phase(PATCH_PARENTS_1) { irFile.acceptVoid(PatchDeclarationParentsVisitor()) }

            phase(LOCAL_DECLARATIONS) {
                LocalDeclarationsLowering(
                    context,
                    object : LocalNameProvider {
                        override fun localName(descriptor: DeclarationDescriptor): String =
                            NameUtils.sanitizeAsJavaIdentifier(super.localName(descriptor))
                    },
                    Visibilities.PUBLIC, //TODO properly figure out visibility
                    true
                ).runOnFilePostfix(irFile)
            }
            phase(CALLABLE_REFERENCE) { CallableReferenceLowering(context).lower(irFile) }
            phase(FUNCTIONN_VARARG_INVOKE) { FunctionNVarargInvokeLowering(context).runOnFilePostfix(irFile) }

            phase(INNER_CLASSES) { InnerClassesLowering(context).runOnFilePostfix(irFile) }
            phase(INNER_CLASS_CONSTRUCTOR_CALLS) { InnerClassConstructorCallsLowering(context).runOnFilePostfix(irFile) }

            phase(PATCH_PARENTS_2) { irFile.acceptVoid(PatchDeclarationParentsVisitor()) }

            phase(ENUM_CLASS) { EnumClassLowering(context).runOnFilePostfix(irFile) }
            //Should be before SyntheticAccessorLowering cause of synthetic accessor for companion constructor
            phase(OBJECT_CLASS) { ObjectClassLowering(context).lower(irFile) }
            phase(INITIALIZERS) {
                InitializersLowering(context, JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER, true).runOnFilePostfix(irFile)
            }
            phase(SINGLETON_REFERENCES) { SingletonReferencesLowering(context).runOnFilePostfix(irFile) }
            phase(SYNTHETIC_ACCESSOR) { SyntheticAccessorLowering(context).lower(irFile) }
            phase(BRIDGE) { BridgeLowering(context).runOnFilePostfix(irFile) }
            phase(JVM_OVERLOADS_ANNOTATION) { JvmOverloadsAnnotationLowering(context).runOnFilePostfix(irFile) }
            phase(JVM_STATIC_ANNOTATION) { JvmStaticAnnotationLowering(context).lower(irFile) }
            phase(STATIC_DEFAULT_FUNCTION) { StaticDefaultFunctionLowering(context.state).runOnFilePostfix(irFile) }

            phase(TAILREC) { TailrecLowering(context).runOnFilePostfix(irFile) }
            phase(TO_ARRAY) { ToArrayLowering(context).runOnFilePostfix(irFile) }

            phase(PATCH_PARENTS_3) { irFile.acceptVoid(PatchDeclarationParentsVisitor()) }

            phase(END_LOWERING) {}
        }
    }
}
