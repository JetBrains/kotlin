import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Annotation

Retention(RetentionPolicy.RUNTIME) annotation class foo(val name : String)

class Test() {
    foo("OK") fun hello(input : String) {
    }
}

fun box(): String {
    val test = Test()
    for (method in javaClass<Test>().getMethods()!!) {
        val anns = method?.getAnnotations() as Array<Annotation>
        if (!anns.isEmpty()) {
            for (ann in anns) {
                val fooAnn = ann as foo
                return fooAnn.name
            }
        }
    }
    return "fail"
}
