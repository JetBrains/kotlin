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

import org.jetbrains.kotlin.backend.common.lower.KCallableNamePropertyLowering
import org.jetbrains.kotlin.backend.common.lower.LateinitLowering
import org.jetbrains.kotlin.backend.common.lower.LocalFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.SharedVariablesLowering
import org.jetbrains.kotlin.backend.common.lower.TailrecLowering
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.lower.*
import org.jetbrains.kotlin.ir.declarations.IrFile

class JvmLower(val context: JvmBackendContext) {
    fun lower(irFile: IrFile) {
        // TODO run lowering passes as callbacks in bottom-up visitor
        FileClassLowering(context).lower(irFile)
        KCallableNamePropertyLowering(context).lower(irFile)
        LateinitLowering(context).lower(irFile)
        ConstAndJvmFieldPropertiesLowering().lower(irFile)
        PropertiesLowering().lower(irFile)
        InterfaceLowering(context.state).runOnFilePostfix(irFile)
        InterfaceDelegationLowering(context.state).runOnFilePostfix(irFile)
        SharedVariablesLowering(context).runOnFilePostfix(irFile)
        InnerClassesLowering(context).runOnFilePostfix(irFile)
        InnerClassConstructorCallsLowering(context).runOnFilePostfix(irFile)
        LocalFunctionsLowering(context).runOnFilePostfix(irFile)
        EnumClassLowering(context).runOnFilePostfix(irFile)
        ObjectClassLowering(context).runOnFilePostfix(irFile)
        InitializersLowering(context).runOnFilePostfix(irFile)
        SingletonReferencesLowering(context).runOnFilePostfix(irFile)
        SyntheticAccessorLowering(context.state).lower(irFile)
        BridgeLowering(context.state).runOnFilePostfix(irFile)

        TailrecLowering(context).runOnFilePostfix(irFile)
    }
}