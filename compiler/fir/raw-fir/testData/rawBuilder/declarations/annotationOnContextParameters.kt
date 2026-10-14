context(@Ann a: @Ann String)
fun annotationOnContext() {}

context(@AnnotationWithConstructor("") a: @AnnotationWithConstructor("") String)
fun annotationWithConstructor() {}

context(@Ann a: @Ann String)
val annotationOnContextProperty: String
    get() = ""

context(@AnnotationWithConstructor("") a: @AnnotationWithConstructor("") String)
val annotationWithConstructorProperty: String
    get() = ""

fun functionType(f: context(@Ann String) () -> Unit) {}

fun functionTypeWithConstructor(f: context(@AnnotationWithConstructor("") String) () -> Unit) {}

fun functionTypeNamed(f: context(c: @Ann String) () -> Unit) {}

fun functionTypeNamedWithConstructor(f: context(c: @AnnotationWithConstructor("") String) () -> Unit) {}