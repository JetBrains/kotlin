// TARGET_BACKEND: JVM

// WITH_STDLIB

import java.lang.annotation.Annotation

@Retention(AnnotationRetention.RUNTIME)
annotation class foo(val name : String)

class Test() {
    @foo("OK") fun hello(input : String) {
    }
}

fun box(): String {
    val test = Test()
    for (method in Test::class.java.getMethods()!!) {
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
