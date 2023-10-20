// This test is for the case when expect annotation is FirAnnotationCall and actual annotation is not FirAnnotationCall
// MODULE: common
// TARGET_PLATFORM: Common
expect annotation class Ann(val p: Int)

@Ann(p = 1)
expect class Foo

// MODULE: main()()(common)
// TARGET_PLATFORM: JVM
// FILE: Foo.kt
actual annotation class Ann actual constructor(actual val p: Int)

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual typealias Foo = FooImpl<!>

// FILE: FooImpl.java
@Ann(p = 2)
public class FooImpl {
}
