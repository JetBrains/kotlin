//  test.pkg.Foo
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

package test.pkg

annotation class A

annotation class B

class Foo @JvmOverloads @A constructor(i: Int = 0) {
    @B
    constructor(s: String = "", i: Int = 0): this(i)
}
