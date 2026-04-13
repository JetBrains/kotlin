// JVM_TARGET: 17
// LIBRARY_PLATFORMS: JVM

package pkg

@JvmRecord
data class MyRec(@property:Ann val name: String, @field:Ann val age: Int, @param:Ann val gender: String) {
    constructor(name: String) : this(name, 0, "unknown")
}

annotation class Ann
