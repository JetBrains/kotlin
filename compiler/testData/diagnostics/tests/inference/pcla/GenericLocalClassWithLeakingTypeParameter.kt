// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-60855
/* ATTENTION:
 * this test monitors an unfixed compiler bug;
 * if the behavior of the test changes, please consult with the linked YT ticket
 * to check whether the described problem has been fixed by your changes;
 * if the issue isn't actually fixed but new behavior persists,
 * please add a comment about the behavior change to the ticket
 * (preferably accompanied by an analysis of the change's reasons)
 */

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = reference <!UNCHECKED_CAST!>as CT<!>
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

private var reference: Any? = null
val <T> Buildee<T>.typeArgumentValue: T get() = reference <!UNCHECKED_CAST!>as T<!>

class UserKlass

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    fun testTypeInfoOriginInsideLocalClass() {
        val buildee = <!CANNOT_INFER_PARAMETER_TYPE!>build<!> {
            class Local<T> {
                fun localOnlyFunc(): T = UserKlass() <!UNCHECKED_CAST!>as T<!>

                fun initialize() {
                    reference = Local<T>()

                    val arg: Local<T> = Local()
                    yield(arg)
                }
            }
            Local<UserKlass>().initialize()
        }
        val result = buildee.<!CANNOT_INFER_PARAMETER_TYPE!>typeArgumentValue<!>.<!UNRESOLVED_REFERENCE!>localOnlyFunc<!>()
        <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: localOnlyFunc")!>result<!>
    }

    fun testThisExpression() {
        val buildee = <!CANNOT_INFER_PARAMETER_TYPE!>build<!> {
            class Local<T> {
                fun localOnlyFunc(): T = UserKlass() <!UNCHECKED_CAST!>as T<!>

                fun initialize() {
                    reference = this

                    yield(this)
                }
            }
            Local<UserKlass>().initialize()
        }
        val result = buildee.<!CANNOT_INFER_PARAMETER_TYPE!>typeArgumentValue<!>.<!UNRESOLVED_REFERENCE!>localOnlyFunc<!>()
        <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: localOnlyFunc")!>result<!>
    }

    testTypeInfoOriginInsideLocalClass()
    testThisExpression()
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun testTypeInfoOriginInsideLocalClass() {
        val buildee = build {
            class Local<T> {
                fun localOnlyFunc(): T = UserKlass() <!UNCHECKED_CAST!>as T<!>

                fun initialize() {
                    reference = Local<T>()

                    fun consume(arg: Local<T>) {}
                    consume(materialize())
                }
            }
            Local<UserKlass>().initialize()
        }
        val result = buildee.typeArgumentValue.localOnlyFunc()
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>result<!>
    }

    fun testThisExpression() {
        val buildee = <!CANNOT_INFER_PARAMETER_TYPE!>build<!> {
            class Local<T> {
                fun localOnlyFunc(): T = UserKlass() <!UNCHECKED_CAST!>as T<!>

                fun initialize() {
                    reference = this

                    fun <T> shareTypeInfo(from: T, to: T) {}
                    <!CANNOT_INFER_PARAMETER_TYPE!>shareTypeInfo<!>(this, materialize())
                }
            }
            Local<UserKlass>().initialize()
        }
        val result = buildee.<!CANNOT_INFER_PARAMETER_TYPE!>typeArgumentValue<!>.<!UNRESOLVED_REFERENCE!>localOnlyFunc<!>()
        <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: localOnlyFunc")!>result<!>
    }

    testTypeInfoOriginInsideLocalClass()
    testThisExpression()
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}

/* GENERATED_FIR_TAGS: asExpression, assignment, classDeclaration, functionDeclaration, functionalType, getter,
lambdaLiteral, localClass, localFunction, localProperty, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, stringLiteral, thisExpression, typeParameter, typeWithExtension */
