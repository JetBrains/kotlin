@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
public annotation class Annotation(val name: String)
fun x() {}

@Annotation(<expr>"y"</expr>)
x()