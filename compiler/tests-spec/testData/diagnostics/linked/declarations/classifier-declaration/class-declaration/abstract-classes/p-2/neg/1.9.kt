// FIR_IDENTICAL
// !LANGUAGE: +ProhibitInvisibleAbstractMethodsInSuperclasses
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT
// FULL_JDK

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: inheritance, overriding -> paragraph 7 -> sentence 1
 * NUMBER: 9
 * DESCRIPTION: Abstract classes may contain one or more abstract members, which should be implemented in a subtype of this abstract class
 * ISSUES: KT-27825, KT-35325
 */

// MODULE: libModule
// FILE: libModule/BaseJava1.java
package libModule;

public abstract class BaseJava1 {
    /*package-private*/ abstract void boojava();
}


// FILE: BaseKotlin1.kt
package libModule

public abstract class BaseKotlin1 {
    internal abstract fun bookotlin()
}

// MODULE: mainModule(libModule)
// FILE: mainModule/JavaClassWithAbstractKotlinClass.java
package mainModule
import libModule.*

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-35325
 */
public class JavaClassWithAbstractKotlinClass {

    public void zoo()
    {
        //todo: INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER should be expected
        BaseKotlin1 baseKotlin = new BaseKotlin1() {};

        BaseKotlin1 baseKotlin = new BaseKotlinImpl();
        baseKotlin.bookotlin$libModule(); //highlight Error: usage of Kotlin internal declaration from different module

        BaseKotlinImpl baseKotlinImpl = new BaseKotlinImpl();
        baseKotlinImpl.bookotlin$libModule();  //there is no any Error, but should be
    }

    class BaseKotlinImpl extends BaseKotlin1 {
        @Override
        public void bookotlin$libModule() {
            System.out.println("wefrgth");
        }
    }

}

// FILE: KotlinClassWithAbstractJavaClass.kt
package mainModule
import libModule.*

// TESTCASE NUMBER: 2
class KotlinClassWithAbstractJavaClass() {
    fun foo() {
        val baseJava1 = <!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR!>object<!> : BaseJava1() {}
        val baseKotlin = <!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR!>object<!> : BaseKotlin1() {}
    }
}
