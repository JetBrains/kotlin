// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header interface A<T> {
    val x: T
    var y: List<T>
    fun f(p: Collection<T>): Map<T, A<T?>>
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl interface A<T> {
    impl val x: T
    impl var y: List<T>
    impl fun f(p: Collection<T>): Map<T, A<T?>>
}
