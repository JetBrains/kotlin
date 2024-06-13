// RENDER_DIAGNOSTICS_MESSAGES
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE +UNUSED_VALUE

@Target(AnnotationTarget.PROPERTY,  AnnotationTarget.FUNCTION, AnnotationTarget.TYPE,  AnnotationTarget.LOCAL_VARIABLE)
annotation class A

@A
fun test() {
    @A
    var b: @A Int = 0
    b = 15
}
