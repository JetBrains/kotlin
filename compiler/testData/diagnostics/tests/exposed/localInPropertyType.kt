class My<T>(val value: T)

open class Base

val <!EXPOSED_PROPERTY_TYPE!>invalid1<!> = run {
    class Local
    My(Local())
}

val <!EXPOSED_PROPERTY_TYPE!>invalid2<!> = My(object {})

val <!EXPOSED_PROPERTY_TYPE!>invalid3<!> = My(object : Base() {})

val <!EXPOSED_PROPERTY_TYPE!>invalid4<!> = run {
    class Local
    My(My(Local()))
}

val <!EXPOSED_PROPERTY_TYPE!>invalid5<!> = run {
    fun invalid5a() = run {
        class Local
        Local()
    }
    My(invalid5a())
}

// Valid: effectively Any
val valid1 = object {}

// Valid: effectively Base
val valid2 = object : Base() {}

// Valid: explicit type argument
val valid3 = My<Base>(object : Base() {})

// Valid: explicit type specified
val valid4 : My<Base> = My(object : Base() {})

// Valid: local class denotable in local scope
val valid5 = run {
    class Local
    fun valid5a() = My(Local())
    My<Any>(valid5a())
}

// Valid: local class denotable in local scope
val valid6 = run {
    class Local
    fun valid6a() = run {
        fun valid6b() = My(Local())
        valid6b()
    }
    My<Any>(valid6a())
}

// Valid: effectively My<Any>
val valid7 = run {
    class Local
    My<My<*>>(My(Local()))
}