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
        NestedInheritor
        // [UNRESOLVED_REFERENCE] Unresolved reference 'NestedInheritor'.
        NestedInheritor
    } catch (e: E1) {
        myClassProp
        // [UNRESOLVED_REFERENCE] Unresolved reference 'myClassProp'.
        myClassProp
    } catch (e: E2) {
        stringProp
        // [UNRESOLVED_REFERENCE] Unresolved reference 'stringProp'.
        stringProp
        // [ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is 'String', but 'MyClass' was expected.
    } finally {
        ClassMemberAlias
    }

    receive<MyClass>(
        try {
            NestedInheritor
            NestedInheritor
        } catch(e: E1) {
            myClassProp
            myClassProp
        } catch (e: E2) {
            stringProp
            stringProp
        } catch (e: E3) {
            getNestedInheritor()
            getNestedInheritor()
        } finally {
            ClassMemberAlias
        }
    )

    run<MyClass> {
        try {
            NestedInheritor
            NestedInheritor
        } catch(e: E1) {
            myClassProp
            myClassProp
        } catch(e: E2) {
            stringProp
            stringProp
        } catch(e: E3) {
            getNestedInheritor()
            getNestedInheritor()
        } finally {
            ClassMemberAlias
        }
    }
}
