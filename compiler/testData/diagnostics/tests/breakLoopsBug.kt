// IGNORE_REVERSED_RESOLVE
// ISSUE: KT-71966

// FILE: A.kt

package com.jetbrains.cidr.lang.fixtures

open class OCDelegatingCodeInsightTestCase {
    open class Inner : OCDelegatingCodeInsightTestCase() {}
}

// FILE: B.kt

class OCDelegatingCodeInsightTestCase : com.jetbrains.cidr.lang.fixtures.OCDelegatingCodeInsightTestCase.Inner()
