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

package org.jetbrains.kotlin.effectsystem.visitors

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.effectsystem.factories.createConstant
import org.jetbrains.kotlin.effectsystem.impls.*
import org.jetbrains.kotlin.effectsystem.structure.EffectSchema
import org.jetbrains.kotlin.effectsystem.structure.calltree.CTCall
import org.jetbrains.kotlin.effectsystem.structure.calltree.CTConstant
import org.jetbrains.kotlin.effectsystem.structure.calltree.CTLambda
import org.jetbrains.kotlin.effectsystem.structure.calltree.CTVariable
import org.jetbrains.kotlin.effectsystem.factories.pureSchema
import org.jetbrains.kotlin.effectsystem.factories.schemaForConstant
import org.jetbrains.kotlin.effectsystem.structure.calltree.CallTreeVisitor

/**
 * Builds effect schema, which describes effects of given call tree
 */
class SchemaBuilder : CallTreeVisitor<EffectSchema?> {
    override fun visitCall(ctCall: CTCall): EffectSchema? {
        val builtArgs = ctCall.arguments.map { it.accept(this) ?: return null }
        return ctCall.functor.apply(builtArgs)
    }

    override fun visitConstant(ctConstant: CTConstant): EffectSchema =
            schemaForConstant(createConstant(ctConstant.id, ctConstant.value, ctConstant.type))

    override fun visitVariable(ctVariable: CTVariable): EffectSchema {
        val variable = if (ctVariable.type == DefaultBuiltIns.Instance.booleanType) {
            ESBooleanVariable(ctVariable.id)
        }
        else {
            ESVariable(ctVariable.id, ctVariable.type)
        }

        return pureSchema(variable)
    }

    override fun visitLambda(ctLambda: CTLambda): EffectSchema =
            pureSchema(ESLambda(ctLambda.id, ctLambda.type))
}