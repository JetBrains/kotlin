// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

open class MyClass {
    object NestedInheritor : MyClass()

    companion object {
        val myClassProp: MyClass = MyClass()
        val stringProp: String = ""
        fun getNestedInheritor() = NestedInheritor
    }
}

val ClassMemberAlias = MyClass.NestedInheritor

class E1(m: String): Exception(m)
class E2(m: String): Exception(m)
class E3(m: String): Exception(m)

fun <T>receive(e: T) {}
fun <T> run(b: () -> T): T = b()

fun testTryCatch() {
    val c: MyClass = try {
        <!UNRESOLVED_REFERENCE!>NestedInheritor<!>
        // [UNRESOLVED_REFERENCE] Unresolved reference 'NestedInheritor'.
        NestedInheritor
    } catch (e: E1) {
        <!UNRESOLVED_REFERENCE!>myClassProp<!>
        // [UNRESOLVED_REFERENCE] Unresolved reference 'myClassProp'.
        myClassProp
    } catch (e: E2) {
        <!UNRESOLVED_REFERENCE!>stringProp<!>
        // [UNRESOLVED_REFERENCE] Unresolved reference 'stringProp'.
        <!ARGUMENT_TYPE_MISMATCH!>stringProp<!>
        // [ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is 'String', but 'MyClass' was expected.
    } finally {
        ClassMemberAlias
    }

    receive<MyClass>(
        try {
            <!UNRESOLVED_REFERENCE!>NestedInheritor<!>
            NestedInheritor
        } catch(e: E1) {
            <!UNRESOLVED_REFERENCE!>myClassProp<!>
            myClassProp
        } catch (e: E2) {
            <!UNRESOLVED_REFERENCE!>stringProp<!>
            <!ARGUMENT_TYPE_MISMATCH!>stringProp<!>
        } catch (e: E3) {
            <!UNRESOLVED_REFERENCE!>getNestedInheritor<!>()
            <!UNRESOLVED_REFERENCE!>getNestedInheritor<!>()
        } finally {
            ClassMemberAlias
        }
    )

    run<MyClass> {
        try {
            <!UNRESOLVED_REFERENCE!>NestedInheritor<!>
            NestedInheritor
        } catch(e: E1) {
            <!UNRESOLVED_REFERENCE!>myClassProp<!>
            myClassProp
        } catch(e: E2) {
            <!UNRESOLVED_REFERENCE!>stringProp<!>
            <!ARGUMENT_TYPE_MISMATCH!>stringProp<!>
        } catch(e: E3) {
            <!UNRESOLVED_REFERENCE!>getNestedInheritor<!>()
            <!UNRESOLVED_REFERENCE!>getNestedInheritor<!>()
        } finally {
            ClassMemberAlias
        }
    }
}
