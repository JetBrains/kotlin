// common.pack.CommonChild
// MODULE: main-common
// FILE: common.kt
package common.pack

expect open class MPPSuper()
class CommonChild : MPPSuper()

// MODULE: m1-jvm()()(main-common)
// FILE: jvm.kt
actual open class MPPSuper actual constructor() {
    fun hello() = "hi" // extra function on Jvm !!
}
