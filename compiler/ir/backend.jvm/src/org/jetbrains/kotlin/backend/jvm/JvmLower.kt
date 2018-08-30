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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.PatchDeclarationParentsVisitor
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.NameUtils

class JvmLower(val context: JvmBackendContext) {
    fun lower(irFile: IrFile) {
        // TODO run lowering passes as callbacks in bottom-up visitor
        FileClassLowering(context).lower(irFile)
        KCallableNamePropertyLowering(context).lower(irFile)

        LateinitLowering(context, true).lower(irFile)

        ConstAndJvmFieldPropertiesLowering(context).lower(irFile)
        PropertiesLowering().lower(irFile)
        AnnotationLowering().runOnFilePostfix(irFile) //should be run before defaults lowering

        //Should be before interface lowering
        DefaultArgumentStubGenerator(context, false).runOnFilePostfix(irFile)

        InterfaceLowering(context).runOnFilePostfix(irFile)
        InterfaceDelegationLowering(context).runOnFilePostfix(irFile)
        SharedVariablesLowering(context).runOnFilePostfix(irFile)

        irFile.acceptVoid(PatchDeclarationParentsVisitor())

        LocalDeclarationsLowering(
            context,
            object : LocalNameProvider {
                override fun localName(descriptor: DeclarationDescriptor): String =
                    NameUtils.sanitizeAsJavaIdentifier(super.localName(descriptor))
            },
            Visibilities.PUBLIC, //TODO properly figure out visibility
            true
        ).runOnFilePostfix(irFile)
        CallableReferenceLowering(context).lower(irFile)

        InnerClassesLowering(context).runOnFilePostfix(irFile)
        InnerClassConstructorCallsLowering(context).runOnFilePostfix(irFile)

        irFile.acceptVoid(PatchDeclarationParentsVisitor())

        EnumClassLowering(context).runOnFilePostfix(irFile)
        //Should be before SyntheticAccessorLowering cause of synthetic accessor for companion constructor
        ObjectClassLowering(context).lower(irFile)
        InitializersLowering(context, JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER, true).runOnFilePostfix(irFile)
        SingletonReferencesLowering(context).runOnFilePostfix(irFile)
        SyntheticAccessorLowering(context).lower(irFile)
        BridgeLowering(context).runOnFilePostfix(irFile)
        JvmOverloadsAnnotationLowering(context).runOnFilePostfix(irFile)
        JvmStaticAnnotationLowering(context).lower(irFile)
        StaticDefaultFunctionLowering(context.state).runOnFilePostfix(irFile)

        TailrecLowering(context).runOnFilePostfix(irFile)
        ToArrayLowering(context).runOnFilePostfix(irFile)

        irFile.acceptVoid(PatchDeclarationParentsVisitor())
    }
}
