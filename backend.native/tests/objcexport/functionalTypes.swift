/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

private func test1() {
    FunctionalTypesKt.callStaticType2(fct: foo2, param: "from swift")
    FunctionalTypesKt.callDynType2(list: [ foo2 ], param: "from swift")

    FunctionalTypesKt.callStaticType2(fct : {a1, _ in return a1 }, param: "from swift block")
    FunctionalTypesKt.callDynType2(list: [ {a1, _ in return a1 } ], param: "from swift block")

    // 32 params is mapped as regular; block is OK
    FunctionalTypesKt.callStaticType32(fct : {
        a1, _, _, _, _, _, _, _,
         _, _, _, _, _, _, _, _,
         _, _, _, _, _, _, _, _,
         _, _, _, _, _, _, _, _
        in return a1 }, param: "from swift block")

    FunctionalTypesKt.callDynType32(list : [{
        a1, _, _, _, _, _, _, _,
         _, _, _, _, _, _, _, _,
         _, _, _, _, _, _, _, _,
         _, _, _, _, _, _, _, _
        in return a1 }], param: "from swift block")

    // 33 params requires explicit implementation of KotlinFunction33
    FunctionalTypesKt.callStaticType33(fct: foo33, param: "from swift")
    FunctionalTypesKt.callDynType33(list: [ Foo33() ], param: "from swift")
}

class FunctionalTypesTests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
    }
}

private func foo2(a1: Any?, _: Any?) -> Any? {
    return a1
}

private func foo33(a1: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?,
        _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?,
        _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?,
        _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?
) -> Any? {
    return a1
}

private class Foo33 : KotlinFunction33 {
    func invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?,
    p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?, p18: Any?, p19: Any?,
    p20: Any?, p21: Any?, p22: Any?, p23: Any?, p24: Any?, p25: Any?, p26: Any?, p27: Any?, p28: Any?, p29: Any?,
    p30: Any?, p31: Any?, p32: Any?, p33: Any?
    ) -> Any? {
        return foo33(a1: p1
                   , nil, nil, nil, nil, nil, nil, nil, nil
                   , nil, nil, nil, nil, nil, nil, nil, nil
                   , nil, nil, nil, nil, nil, nil, nil, nil
                   , nil, nil, nil, nil, nil, nil, nil, nil)
    }
}
