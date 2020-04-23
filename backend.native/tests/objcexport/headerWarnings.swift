/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

// Note: the test parses the generated header with -Werror to detect warnings.
// It is enough to have just Kotlin declarations at the moment.
// Adding usages for all declarations to avoid any kind of DCE that may appear later.

private func testIncompatiblePropertyType() throws {
    let c = TestIncompatiblePropertyTypeWarning.ClassOverridingInterfaceWithGenericProperty(
            p: TestIncompatiblePropertyTypeWarningGeneric<NSString>(value: "cba")
    )

    let pc: TestIncompatiblePropertyTypeWarningGeneric<NSString> = c.p
    try assertEquals(actual: pc.value, expected: "cba")

    let i: TestIncompatiblePropertyTypeWarningInterfaceWithGenericProperty = c
    let pi: TestIncompatiblePropertyTypeWarningGeneric<AnyObject> = i.p
    try assertEquals(actual: pi.value as! String, expected: "cba")
}

class HeaderWarningsTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestIncompatiblePropertyType", testIncompatiblePropertyType)
    }
}