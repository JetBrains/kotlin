// !DIAGNOSTICS: -JAVA_LANG_CLASS_PARAMETER_IN_ANNOTATION
package test

annotation class AnnClass(val a: Class<*>)

class MyClass {

    AnnClass(javaClass<MyClass>()) companion object {
    }

}
