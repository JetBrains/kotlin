package test

fun findClassOrFail(className: String): Class<*> =
        try {
            Class.forName(className)
        }
        catch (e: Exception) {
            throw AssertionError("Class $className not found")
        }

fun box(): String {
    val testPackage = findClassOrFail("test.TestPackage")
    val deprecated = findClassOrFail("java.lang.Deprecated") as Class<Annotation>
    val ann = testPackage.getAnnotation(deprecated)
    assert(ann != null) { "Package facade ${testPackage.name} is not deprecated" }

    return "OK"
}