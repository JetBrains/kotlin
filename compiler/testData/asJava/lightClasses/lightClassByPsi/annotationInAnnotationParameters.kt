// FILE: a/A.kt

package a

annotation class A


// FILE: b/B.kt

package b

import a.A

annotation class B(val param: A)

@B(param = A())
class C