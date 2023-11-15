// FILE: kotlin.kt
package kotlin

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
public annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)

public open class Exception : Throwable()

public open class RuntimeException : Exception()

public open class IllegalStateException : RuntimeException()

// FILE: native.kt
package kotlin.native

@Deprecated("")
public typealias Throws = kotlin.Throws

// FILE: CancellationException.kt
package kotlin.coroutines.cancellation

public open class CancellationException() : IllegalStateException()

// FILE: test.kt
import kotlin.coroutines.cancellation.CancellationException

class Exception1 : Throwable()
class Exception2 : Throwable()
class Exception3 : Throwable()

<!THROWS_LIST_EMPTY!>@Throws<!>
fun foo() {}

<!THROWS_LIST_EMPTY!>@Throws()<!>
fun throwsEmptyParens() {}

@Throws(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>UnresolvedException<!>::class<!>)
fun throwsUnresolved() {}

class Orphan : <!UNRESOLVED_REFERENCE!>MyUnresolvedParent<!>
@Throws(<!ARGUMENT_TYPE_MISMATCH!>Orphan::class<!>)
fun throwsClassWithUnresolvedParent() {}

@Throws(exceptionClasses = <!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH, ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION_ERROR!><!UNRESOLVED_REFERENCE!>UnresolvedException<!>::class<!>)
fun throwsNamedUnresolved() {}

<!THROWS_LIST_EMPTY!>@Throws(exceptionClasses = [])<!>
fun throwsNamedEmptyLiteral() {}

<!THROWS_LIST_EMPTY!>@Throws(exceptionClasses = arrayOf())<!>
fun throwsNamedEmptyArrayOf() {}

<!THROWS_LIST_EMPTY!>@Throws(*[])<!>
fun throwsSpreadEmptyLiteral() {}

<!THROWS_LIST_EMPTY!>@Throws(*arrayOf())<!>
fun throwsSpreadEmptyArrayOf() {}

@Throws(exceptionClasses = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>UE<!>::class<!>]<!>)
fun throwsNamedLiteralWithUnresolved() {}

@Throws(exceptionClasses = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>arrayOf(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>UE<!>::class<!>)<!>)
fun throwsNamedArrayOfUnresolved() {}

@Throws(*<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>UE<!>::class<!>]<!>)
fun throwsSpreadLiteralWithUnresolved() {}

@Throws(*<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>arrayOf(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>UE<!>::class<!>)<!>)
fun throwsSpreadArrayOfUnresolved() {}

typealias UEAlias = <!UNRESOLVED_REFERENCE!>UE<!>

@Throws(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>UEAlias::class<!>)
fun throwsTypealiasToUnresolved() {}

interface Base0 {
    fun foo()
}

class ThrowsOnOverride : Base0 {
    <!INCOMPATIBLE_THROWS_OVERRIDE!>@Throws(Exception1::class)<!> override fun foo() {}
}

interface Base1 {
    @Throws(Exception1::class) fun foo()
}

class InheritsThrowsAndNoThrows : Base0, Base1 {
    <!INCOMPATIBLE_THROWS_INHERITED!>override fun foo() {}<!>
}

class OverridesThrowsAndNoThrows : Base0, Base1 {
    <!INCOMPATIBLE_THROWS_INHERITED!>@Throws(Exception1::class) override fun foo() {}<!>
}

class SameThrowsOnOverride : Base1 {
    @Throws(Exception1::class) override fun foo() {}
}

class DifferentThrowsOnOverride : Base1 {
    <!INCOMPATIBLE_THROWS_OVERRIDE!>@Throws(Exception2::class)<!> override fun foo() {}
}

class HasThrowsWithEmptyListOnOverride : Base1 {
    <!INCOMPATIBLE_THROWS_OVERRIDE!>@Throws<!> override fun foo() {}
}

interface Base2 {
    @Throws(Exception2::class) fun foo()
}

open class InheritsDifferentThrows1 : Base1, Base2 {
    <!INCOMPATIBLE_THROWS_INHERITED!>override fun foo() {}<!>
}

open class OverridesDifferentThrows1_1 : Base1, Base2 {
    <!INCOMPATIBLE_THROWS_INHERITED!>@Throws(Exception1::class) override fun foo() {}<!>
}

open class OverridesDifferentThrows1_2 : Base1, Base2 {
    <!INCOMPATIBLE_THROWS_INHERITED!>@Throws(Exception2::class) override fun foo() {}<!>
}

open class OverridesDifferentThrows1_3 : Base1, Base2 {
    <!INCOMPATIBLE_THROWS_INHERITED!>@Throws(Exception1::class, Exception2::class) override fun foo() {}<!>
}

class InheritsDifferentThrowsThroughSameClass1 : InheritsDifferentThrows1() {
    <!INCOMPATIBLE_THROWS_INHERITED!>override fun foo() {}<!>
}

class OverridesDifferentThrowsThroughSameClass1 : InheritsDifferentThrows1() {
    <!INCOMPATIBLE_THROWS_INHERITED!>@Throws(Exception1::class) override fun foo() {}<!>
}

class OverridesDifferentThrowsThroughSameClass2 : InheritsDifferentThrows1() {
    <!INCOMPATIBLE_THROWS_INHERITED!>@Throws(Exception2::class) override fun foo() {}<!>
}

interface Base3 {
    @Throws(Exception3::class) fun foo()
}

class InheritsDifferentThrows2 : InheritsDifferentThrows1(), Base3 {
    <!INCOMPATIBLE_THROWS_INHERITED!>override fun foo() {}<!>
}

class OverridesDifferentThrows2 : InheritsDifferentThrows1(), Base3 {
    <!INCOMPATIBLE_THROWS_INHERITED!>@Throws(Exception3::class) override fun foo() {}<!>
}

open class OverridesDifferentThrows3 : Base1, Base2 {
    <!INCOMPATIBLE_THROWS_INHERITED!>@Throws(Exception3::class) override fun foo() {}<!>
}

class InheritsDifferentThrows3 : OverridesDifferentThrows3() {
    override fun foo() {}
}

class OverrideDifferentThrows4 : OverridesDifferentThrows3() {
    override fun foo() {}
}

class OverrideDifferentThrows5 : OverridesDifferentThrows3() {
    @Throws(Exception3::class) override fun foo() {}
}

class OverrideDifferentThrows6 : OverridesDifferentThrows3() {
    <!INCOMPATIBLE_THROWS_OVERRIDE!>@Throws(Exception1::class)<!> override fun foo() {}
}

interface Base4 {
    @Throws(Exception1::class) fun foo()
}

class InheritsSameThrows : Base1, Base4 {
    override fun foo() {}
}

class OverridesSameThrows : Base1, Base4 {
    @Throws(Exception1::class) override fun foo() {}
}

class OverrideDifferentThrows7 : Base1, Base4 {
    <!INCOMPATIBLE_THROWS_OVERRIDE!>@Throws(Exception2::class)<!> override fun foo() {}
}

class OverrideDifferentThrows8 : Base1, Base3 {
    <!INCOMPATIBLE_THROWS_INHERITED!>@Throws(Exception2::class) override fun foo() {}<!>
}

interface Base5 {
    @Throws(Exception1::class, Exception2::class) fun foo()
}

interface Base6 {
    @Throws(Exception2::class, Exception1::class) fun foo()
}

class InheritsSameThrowsMultiple : Base5, Base6 {
    override fun foo() {}
}

class OverridesSameThrowsMultiple1 : Base5, Base6 {
    @Throws(Exception1::class, Exception2::class) override fun foo() {}
}

class OverridesSameThrowsMultiple2 : Base5, Base6 {
    @Throws(Exception2::class, Exception1::class) override fun foo() {}
}

class OverridesDifferentThrowsMultiple : Base5, Base6 {
    <!INCOMPATIBLE_THROWS_OVERRIDE!>@Throws(Exception1::class)<!> override fun foo() {}
}

fun withLocalClass() {
    class LocalException : Throwable()

    abstract class Base7 {
        @Throws(Exception1::class, LocalException::class) abstract fun foo()
    }

    class InheritsDifferentThrowsLocal : Base1, Base7() {
        <!INCOMPATIBLE_THROWS_INHERITED!>override fun foo() {}<!>
    }

    class OverridesDifferentThrowsLocal : Base1, Base7() {
        <!INCOMPATIBLE_THROWS_INHERITED!>@Throws(Exception1::class, LocalException::class) override fun foo() {}<!>
    }
}

interface ThrowsOnFakeOverride : Base1

class InheritThrowsOnFakeOverride : ThrowsOnFakeOverride {
    override fun foo() {}
}

class OverrideDifferentThrowsOnFakeOverride : ThrowsOnFakeOverride {
    <!INCOMPATIBLE_THROWS_OVERRIDE!>@Throws(Exception2::class)<!> override fun foo() {}
}

interface IncompatibleThrowsOnFakeOverride : Base1, Base2

class OverrideIncompatibleThrowsOnFakeOverride1 : IncompatibleThrowsOnFakeOverride {
    <!INCOMPATIBLE_THROWS_INHERITED!>@Throws(Exception1::class) override fun foo() {}<!>
}

class OverrideIncompatibleThrowsOnFakeOverride2 : IncompatibleThrowsOnFakeOverride {
    <!INCOMPATIBLE_THROWS_INHERITED!>@Throws(Exception2::class) override fun foo() {}<!>
}

class InheritIncompatibleThrowsOnFakeOverride : IncompatibleThrowsOnFakeOverride {
    <!INCOMPATIBLE_THROWS_INHERITED!>override fun foo() {}<!>
}

<!THROWS_LIST_EMPTY!>@Throws<!>
suspend fun suspendThrowsNothing() {}

interface SuspendFun {
    suspend fun foo()
}

class OverrideImplicitThrowsOnSuspendWithExplicit : SuspendFun {
    // Although `SuspendFun.foo` effectively has `@Throws(CancellationException::class)`,
    // overriding it with equal explicit `@Throws` is forbidden:
    <!INCOMPATIBLE_THROWS_OVERRIDE!>@Throws(CancellationException::class)<!> override suspend fun foo() {}
}

interface SuspendFunThrows {
    @Throws(CancellationException::class) suspend fun foo() {}
}

class InheritExplicitThrowsOnSuspend : SuspendFunThrows {
    override suspend fun foo() {}
}

<!MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND!>@Throws(Exception1::class)<!>
suspend fun suspendDoesNotThrowCancellationException1() {}

<!MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND!>@Throws(Exception1::class, Exception2::class)<!>
suspend fun suspendDoesNotThrowCancellationException2() {}

@Throws(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>UE<!>::class<!>)
suspend fun suspendThrowsUnresolved() {}

@Throws(exceptionClasses = <!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH, ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION_ERROR!><!UNRESOLVED_REFERENCE!>UE<!>::class<!>)
suspend fun suspendThrowsNamedUnresolved() {}

<!THROWS_LIST_EMPTY!>@Throws(exceptionClasses = [])<!>
suspend fun suspendThrowsNamedEmptyLiteral() {}

<!THROWS_LIST_EMPTY!>@Throws(exceptionClasses = arrayOf())<!>
suspend fun suspendThrowsNamedEmptyArrayOf() {}

<!THROWS_LIST_EMPTY!>@Throws(*[])<!>
suspend fun suspendThrowsSpreadEmptyLiteral() {}

<!THROWS_LIST_EMPTY!>@Throws(*arrayOf())<!>
suspend fun suspendThrowsSpreadEmptyArrayOf() {}

@Throws(exceptionClasses = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>UE<!>::class<!>]<!>)
suspend fun suspendThrowsNamedLiteralWithUnresolved() {}

@Throws(exceptionClasses = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>arrayOf(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>UE<!>::class<!>)<!>)
suspend fun suspendThrowsNamedArrayOfUnresolved() {}

@Throws(*<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>UE<!>::class<!>]<!>)
suspend fun suspendThrowsSpreadLiteralWithUnresolved() {}

@Throws(*<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>arrayOf(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>UE<!>::class<!>)<!>)
suspend fun suspendThrowsSpreadArrayOfUnresolved() {}

@Throws(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>UEAlias::class<!>)
suspend fun suspendThrowsTypealiasToUnresolved() {}

@Throws(<!ARGUMENT_TYPE_MISMATCH!>Orphan::class<!>)
suspend fun suspendThrowsClassWithUnresolvedParent() {}

@Throws(Exception1::class, CancellationException::class)
suspend fun suspendThrowsCancellationException1() {}

@Throws(CancellationException::class, Exception1::class)
suspend fun suspendThrowsCancellationException2() {}

typealias CancellationExceptionAlias = CancellationException

@Throws(CancellationExceptionAlias::class)
suspend fun suspendThrowsCancellationExceptionTypealias() {}

@Throws(IllegalStateException::class)
suspend fun suspendThrowsIllegalStateException1() {}

@Throws(Exception2::class, IllegalStateException::class)
suspend fun suspendThrowsIllegalStateException2() {}

typealias IllegalStateExceptionAlias = IllegalStateException

@Throws(IllegalStateExceptionAlias::class)
suspend fun suspendThrowsIllegalStateExceptionTypealias() {}

@Throws(RuntimeException::class)
suspend fun suspendThrowsRuntimeException1() {}

@Throws(RuntimeException::class, Exception3::class)
suspend fun suspendThrowsRuntimeException2() {}

typealias RuntimeExceptionAlias = RuntimeException

@Throws(RuntimeExceptionAlias::class)
suspend fun suspendThrowsRuntimeExceptionTypealias() {}

@Throws(Exception::class)
suspend fun suspendThrowsException1() {}

@Throws(Exception1::class, Exception::class)
suspend fun suspendThrowsException2() {}

typealias ExceptionAlias = Exception

@Throws(ExceptionAlias::class)
suspend fun suspendThrowsExceptionTypealias() {}

@Throws(Throwable::class)
suspend fun suspendThrowsThrowable1() {}

@Throws(Throwable::class, Exception2::class)
suspend fun suspendThrowsThrowable2() {}

@Throws(Throwable::class, CancellationException::class)
suspend fun suspendThrowsThrowable3() {}

typealias ThrowableAlias = Throwable

@Throws(ThrowableAlias::class)
suspend fun suspendThrowsThrowableTypealias() {}
