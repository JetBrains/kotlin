// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// PARAMETERS_METADATA

// FILE: A.kt

inline class A(val i: Int) {
    fun foo(v: Int) = i + v
}

fun A.bar() = this.i

// A nullable inline class type stays boxed on the JVM, so the parameter already holds a real `A` instance
// and its name must be left alone.
fun baz(nullable: A?, notNull: A) = (nullable?.i ?: 0) + notNull.i

fun box(): String {
    val method = Class.forName("A").declaredMethods.single { it.name == "foo-impl" }
    val parameters = method.getParameters()
    if (parameters[0].name != "\$v\$c\$A\$-this") return "wrong name on receiver parameter: ${parameters[0].name}"
    if (parameters[1].name != "v") return "wrong name on actual parameter: ${parameters[1].name}"

    val extensionMethod = Class.forName("AKt").declaredMethods.single { it.name.startsWith("bar") }
    val extensionMethodParameters = extensionMethod.getParameters()
    if (extensionMethodParameters[0].name != "\$v\$c\$A\$-\$this\$bar")
        return "wrong name on extension receiver parameter: ${extensionMethodParameters[0].name}"

    val nullabilityMethod = Class.forName("AKt").declaredMethods.single { it.name.startsWith("baz") }
    val nullabilityMethodParameters = nullabilityMethod.getParameters()
    if (nullabilityMethodParameters[0].name != "nullable")
        return "wrong name on nullable inline class parameter: ${nullabilityMethodParameters[0].name}"
    if (nullabilityMethodParameters[1].name != "\$v\$c\$A\$-notNull")
        return "wrong name on non-null inline class parameter: ${nullabilityMethodParameters[1].name}"

    return "OK"
}
