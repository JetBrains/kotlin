/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import kotlin.jvm.functions.FunctionN
import kotlin.reflect.KCallable

// copied from core/reflection.jvm/src/kotlin/reflect/jvm/internal/FunctionWithAllInvokes.kt
// additionally added FunctionN
internal interface FunctionWithAllInvokes :
    // (0..22).forEach { n -> println("Function$n<" + (0..n).joinToString { "Any?" } + ">,") }
    Function0<Any?>,
    Function1<Any?, Any?>,
    Function2<Any?, Any?, Any?>,
    Function3<Any?, Any?, Any?, Any?>,
    Function4<Any?, Any?, Any?, Any?, Any?>,
    Function5<Any?, Any?, Any?, Any?, Any?, Any?>,
    Function6<Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function7<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function8<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function9<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function10<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function11<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function12<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function13<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function14<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function15<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function16<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function17<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function18<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function19<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function20<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function21<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    Function22<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>,
    KCallable<Any?>, FunctionN<Any?>
{
    // (0..22).forEach { n -> println("override fun invoke(" + (1..n).joinToString { "p$it: Any?" } + "): Any? = call(" + (1..n).joinToString { "p$it" } + ")") }
    override fun invoke(): Any? = call()
    override fun invoke(p1: Any?): Any? = call(p1)
    override fun invoke(p1: Any?, p2: Any?): Any? = call(p1, p2)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?): Any? = call(p1, p2, p3)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?): Any? = call(p1, p2, p3, p4)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?): Any? = call(p1, p2, p3, p4, p5)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?): Any? = call(p1, p2, p3, p4, p5, p6)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8, p9)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?, p18: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?, p18: Any?, p19: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?, p18: Any?, p19: Any?, p20: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?, p18: Any?, p19: Any?, p20: Any?, p21: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21)
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?, p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?, p18: Any?, p19: Any?, p20: Any?, p21: Any?, p22: Any?): Any? = call(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21, p22)
    override fun invoke(vararg args: Any?): Any? = call(*args)
}
