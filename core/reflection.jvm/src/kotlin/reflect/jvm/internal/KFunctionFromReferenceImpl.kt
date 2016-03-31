/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.Name
import kotlin.jvm.internal.FunctionReference
import kotlin.reflect.KCallable
import kotlin.reflect.KotlinReflectionInternalError

internal class KFunctionFromReferenceImpl(
        val reference: FunctionReference
): KFunctionImpl(
        reference.owner as? KDeclarationContainerImpl ?: EmptyContainerForLocal,
        reference.name,
        reference.signature
) {
    override fun getArity() = reference.arity

    override val name = reference.name

    // The rest of the class is auto-generated. Use the following script:
    // (0..22).forEach { n -> println("override fun invoke(" + (1..n).joinToString { "p$it: Any?" } + "): Any? = reference(" + (1..n).joinToString { "p$it" } + ")") }
    override fun invoke(): Any? = reference()
    override fun invoke(p1: Any?): Any? = reference(p1)
    override fun invoke(p1: Any?, p2: Any?): Any? = reference(p1, p2)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?): Any? = reference(p1, p2, p3)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?): Any? = reference(p1, p2, p3, p4)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?): Any? = reference(p1, p2, p3, p4, p5)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?): Any? = reference(p1, p2, p3, p4, p5, p6)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8, p9)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?, p18: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?, p18: Any?, p19: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?, p18: Any?, p19: Any?, p20: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?, p18: Any?, p19: Any?, p20: Any?, p21: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?, p18: Any?, p19: Any?, p20: Any?, p21: Any?, p22: Any?): Any? = reference(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21, p22)
}

internal object EmptyContainerForLocal : KDeclarationContainerImpl() {
    override val jClass: Class<*>
        get() = fail()

    override val members: Collection<KCallable<*>>
        get() = fail()

    override val constructorDescriptors: Collection<ConstructorDescriptor>
        get() = fail()

    override fun getProperties(name: Name): Collection<PropertyDescriptor> = fail()

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> = fail()

    private fun fail(): Nothing = throw KotlinReflectionInternalError("Introspecting local functions, lambdas and anonymous functions " +
                                                             "is not yet fully supported in Kotlin reflection")
}
