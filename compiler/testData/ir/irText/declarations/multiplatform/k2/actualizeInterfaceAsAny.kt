// LANGUAGE: +MultiPlatformProjects +AllowAnyAsAnActualTypeForExpectInterface

// MODULE: common
// FILE: common.kt
expect interface Marker


open class Test
open class B : Marker {}

interface NoSuperTypeMarker
interface Marker2: Marker
interface Marker3: Marker2, Marker

class C : Test(), NoSuperTypeMarker, Marker {}

fun <T: Marker> test1() {}
fun <T> test2() where T: Marker, T: NoSuperTypeMarker {}
fun <T> test3() where T: Marker?, T: NoSuperTypeMarker {}

// MODULE: jvm()()(common)
// FILE: main.kt
actual typealias Marker = Any
