// SKIP_TXT
// FILE: test.kt

package kotlin.test

annotation class IrrelevantClass

public typealias Test = IrrelevantClass

// FILE: main.kt

import kotlin.test.Test

class A {
    @Test
    <!UNSUPPORTED!>suspend<!> fun test() {}
}

@Test
<!UNSUPPORTED!>suspend<!> fun test() {}
