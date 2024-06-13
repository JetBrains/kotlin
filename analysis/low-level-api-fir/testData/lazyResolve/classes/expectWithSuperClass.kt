// LANGUAGE: +MultiPlatformProjects

expect open class Foo(val obj: Any)

expect class Bar<caret>(obj: Any) : Foo(obj)