fun <T> run(f: () -> T): T {
    return f()
}

class My<T>(val value: T)

open class Base

fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>invalid1<!>() = run {
    class Local
    My(Local())
}

fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>invalid2<!>() = My(object {})

fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>invalid3<!>() = My(object : Base() {})

fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>invalid4<!>() = run {
    class Local
    My(My(Local()))
}

fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>invalid5<!>() = run {
    fun invalid5a() = run {
        class Local
        Local()
    }
    My(invalid5a())
}

// Valid: effectively Any
fun valid1() = object {}

// Valid: effectively Base
fun valid2() = object : Base() {}

// Valid: explicit type argument
fun valid3() = My<Base>(object : Base() {})

// Valid: explicit type specified
fun valid4() : My<Base> = My(object : Base() {})

// Valid: local class denotable in local scope
fun valid5() = run {
    class Local
    fun valid5a() = My(Local())
    My<Any>(valid5a())
}

// Valid: local class denotable in local scope
fun valid6() = run {
    class Local
    fun valid6a() = run {
        fun valid6b() = My(Local())
        valid6b()
    }
    My<Any>(valid6a())
}

// Valid: effectively My<Any>
fun valid7() = run {
    class Local
    My<My<*>>(My(Local()))
}
