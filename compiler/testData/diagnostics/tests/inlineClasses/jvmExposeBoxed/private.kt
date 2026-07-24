// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB
// DIAGNOSTICS: -GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class IC(val s: String)

<!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed("create")<!>
private fun createIC(): IC = TODO()

<!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed<!>
private fun create(ic: IC) {}

<!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed<!>
private class A{}

<!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@get:JvmExposeBoxed("getIC")<!>
<!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@set:JvmExposeBoxed<!>
private var ic: IC = IC("")

<!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@get:JvmExposeBoxed("getIC1")<!>
val ic1: IC
    private get(): IC = TODO()

<!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@set:JvmExposeBoxed<!>
var ic2: IC = IC("")
    private set(ic: IC) {}

var ic3: IC
    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed("getIC3")<!>
    private get(): IC = TODO()
    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed<!>
    private set(ic: IC) {}

class B <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed<!> private constructor(ic: IC) {
    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed<!>
    private fun foo1(): IC = TODO()

    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed<!>
    private fun bar(ic: IC) {}

    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@get:JvmExposeBoxed("getIC")<!>
    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@set:JvmExposeBoxed<!>
    private var ic: IC = IC("")

    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@get:JvmExposeBoxed("getIC1")<!>
    val ic1: IC
        private get(): IC = TODO()

    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@set:JvmExposeBoxed<!>
    var ic2: IC = IC("")
        private set(ic: IC) {}

    var ic3: IC
        <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed("getIC3")<!>
        private get(): IC = TODO()
        <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed<!>
        private set(ic: IC) {}
}

private class c <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed<!> constructor(ic: IC) {
    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed<!>
    fun foo1(): IC = TODO()

    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed<!>
    fun bar(ic: IC) {}

    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@get:JvmExposeBoxed("getIC")<!>
    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@set:JvmExposeBoxed<!>
    var ic: IC = IC("")

    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@get:JvmExposeBoxed("getIC1")<!>
    val ic1: IC
        get(): IC = TODO()

    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@set:JvmExposeBoxed<!>
    var ic2: IC = IC("")
        set(ic: IC) {}

    var ic3: IC
        <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed("getIC3")<!>
        get(): IC = TODO()
        <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed<!>
        set(ic: IC) {}
}

private class C {
    class D {
        <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_PRIVATE!>@JvmExposeBoxed<!> fun foo1(): IC = TODO()
    }
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, annotationUseSiteTargetPropertyGetter,
annotationUseSiteTargetPropertySetter, classDeclaration, classReference, functionDeclaration, getter, primaryConstructor,
propertyDeclaration, setter, stringLiteral, value */
