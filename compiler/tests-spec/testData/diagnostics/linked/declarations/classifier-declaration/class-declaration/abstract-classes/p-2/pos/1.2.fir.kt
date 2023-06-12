// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Abstract classes may contain one or more abstract members, which should be implemented in a subtype of this abstract class
 * HELPERS: checkType, functions
 * ISSUES: KT-27825
 */

// TESTCASE NUMBER: 1

fun case1(){
    OtherClass().zoooo()
    checkSubtype<MainClass.Base2>(OtherClass().zoooo())
}

class MainClass {
    abstract class Base1() {
        abstract val a: CharSequence
        abstract var b: CharSequence

        abstract fun foo(): CharSequence
    }

    abstract class Base2 : Base1() {
        abstract fun boo(x: Int = 10)
    }

}

class OtherClass {

    abstract inner class ImplBase2() : MainClass.Base2() {
        override var b: CharSequence
            get() = TODO()
            set(value) {}
        override val a: CharSequence = ""

        override fun boo(x: Int) {
            TODO()
        }

    }

    fun zoooo(): ImplBase2 {
        val k = object : ImplBase2() {
            override fun foo(): CharSequence = ""
        }
        return k
    }
}
