// RESOLVE_SCRIPT

class Builder {
    var version: String = ""

    @Anno(En.Entry)
    fun execute() {
        println(version)
    }
}

enum class En {
    Entry
}

@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Anno(val s: En)

@Anno(En.Entry)
fun build(action: Builder.() -> Unit) = Builder().apply(action)

@Anno(En.Entry)
build {
    version = "123"
    class A {
        @Anno(En.Entry)
        fun doo(i: Int) {

        }
    }

    execute()
}

val builder = build {
    version = "321"
}

@Anno(En.Entry)
builder.execute()
builder.version = ""
