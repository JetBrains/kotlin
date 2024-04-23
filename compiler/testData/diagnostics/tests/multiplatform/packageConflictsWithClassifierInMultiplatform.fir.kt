// MODULE: m1-common
// FILE: common.kt
package a

class b

// FILE: common2.kt
package c.d

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
package a.b

// FILE: jvm2.kt
package c

class d