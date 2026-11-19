// A dot in the name of a top-level declaration is reported, since it is not supported by the KLIB format
val `MyClass.field` = 1

// A dot in the name of a member declaration is not reported
class MyClass {
    val `MyClass.field` = 2
}
