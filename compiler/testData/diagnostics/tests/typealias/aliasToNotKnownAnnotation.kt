// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-70179

// MODULE: lib
// FILE: some/my/Ann.kt
package some.my;

annotation class Ann()

// MODULE: alias(lib)
// FILE: wrapper/KAnn.kt
package wrapper

import some.my.Ann

public typealias KAnn = Ann

// MODULE: app(alias)
// FILE: test.kt

import wrapper.KAnn

@KAnn
fun foo() {}