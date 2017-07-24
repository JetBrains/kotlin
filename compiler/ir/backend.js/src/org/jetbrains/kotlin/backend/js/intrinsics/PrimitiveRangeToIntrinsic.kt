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

package org.jetbrains.kotlin.backend.js.intrinsics

import org.jetbrains.kotlin.backend.js.context.IrTranslationContext
import org.jetbrains.kotlin.backend.js.expression.translateAsTypeReference
import org.jetbrains.kotlin.backend.js.util.buildJs
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

object PrimitiveRangeToIntrinsic : BinaryOperationIntrinsic() {
    private val rangeClassCache = hashMapOf<PrimitiveType, MutableMap<ModuleDescriptor, ClassDescriptor>>()

    override fun isApplicable(name: String, first: KotlinType, second: KotlinType): Boolean =
            name == "rangeTo" && KotlinBuiltIns.isPrimitiveType(first) && second == first

    override fun apply(context: IrTranslationContext, call: IrCall, first: JsExpression, second: JsExpression): JsExpression {
        val primitiveType = KotlinBuiltIns.getPrimitiveType(call.descriptor.dispatchReceiverParameter!!.type)!!
        val module = context.module.descriptor
        val rangeClass = rangeClassCache.getOrPut(primitiveType) { WeakHashMap() }.getOrPut(module) {
            val className = "kotlin.ranges.${primitiveType.typeName.identifier}Range"
            module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(className)))!!
        }
        return buildJs { context.translateAsTypeReference(rangeClass).newInstance(first, second) }
    }
}
