// RUN_PIPELINE_TILL: BACKEND
class C {
    @Suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION")
    val foo: String?? = ""!! as String??
}