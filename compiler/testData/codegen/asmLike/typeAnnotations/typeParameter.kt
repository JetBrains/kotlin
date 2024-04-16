// RENDER_ANNOTATIONS
// TARGET_BACKEND: JVM_IR

package foo

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class TypeParameterAnn(val name: String)

class Kotlin {

    fun <@TypeParameterAnn("T") T> bar(p: T): T {
        return p
    }

}

