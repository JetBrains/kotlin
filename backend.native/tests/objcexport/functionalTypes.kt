/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package functionalTypes

import kotlin.test.*

typealias AN = Any?

typealias F2 = (AN, AN) -> AN
typealias F5 = (AN, AN, AN, AN, AN) -> AN
typealias F6 = (AN, AN, AN, AN, AN, AN,) -> AN
typealias F32 = (AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN,
                 AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN,
                 AN, AN, AN, AN, AN, AN, AN, AN) -> AN
typealias F33 = (AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN,
                 AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN, AN,
                 AN, AN, AN, AN, AN, AN, AN, AN, AN) -> AN

fun callDynType2(list: List<F2>, param: AN) {
    val fct = list.first()
    val ret = fct(param, null)
    assertEquals(param, ret)
}

fun callStaticType2(fct: F2, param: AN) {
    val ret = fct(param, null)
    assertEquals(param, ret)
}

fun callDynType32(list: List<F32>, param: AN) {
    val fct = list.first()
    val ret = fct(param
            , null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
    )
    assertEquals(param, ret)
}

fun callStaticType32(fct: F32, param: AN) {
    val ret = fct(param
            , null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
    )
    assertEquals(param, ret)
}

fun callDynType33(list: List<F33>, param: AN) {
    val fct = list.first()
    val ret = fct(param
            , null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null, null
    )
    assertEquals(param, ret)
}

fun callStaticType33(fct: F33, param: AN) {
    val ret = fct(param
            , null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null, null
    )
    assertEquals(param, ret)
}
