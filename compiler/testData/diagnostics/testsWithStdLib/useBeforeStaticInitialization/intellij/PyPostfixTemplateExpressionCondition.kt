// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
interface PyPostfixTemplateExpressionCondition {

    val id: String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int

    abstract class PySimpleConditionBase(override val id: String) : PyPostfixTemplateExpressionCondition {

        override fun equals(other: Any?): Boolean {
            return if (this === other) true else other != null
        }

        override fun hashCode(): Int {
            return 0
        }
    }

    class PyBooleanExpression : PySimpleConditionBase(ID) {

        companion object {
            private const val ID: String = "boolean"
        }
    }

    class PyNumberExpression : PySimpleConditionBase(ID) {

        companion object {
            private const val ID: String = "number"
        }
    }

    class PyStringExpression : PySimpleConditionBase(ID) {

        companion object {
            private const val ID: String = "string"
        }
    }

    class PyIterable : PySimpleConditionBase(ID) {

        companion object {
            private const val ID: String = "iterable"
        }
    }

    abstract class PyCollectionTypeConditionBase(private val type: String, private val presentableNameKey: String) : PySimpleConditionBase(type)

    class PyDict : PyCollectionTypeConditionBase("dict", "postfix.template.condition.dict.name")
    class PyList : PyCollectionTypeConditionBase("list", "postfix.template.condition.list.name")
    class PySet : PyCollectionTypeConditionBase("set", "postfix.template.condition.set.name")
    class PyTuple : PyCollectionTypeConditionBase("tuple", "postfix.template.condition.tuple.name")

    class PyNonNoneExpression : PySimpleConditionBase(ID) {

        companion object {
            private const val ID: String = "non none"
        }
    }

    class PyExceptionExpression : PySimpleConditionBase(ID) {

        companion object {
            private const val ID: String = "exception"
        }
    }

    class PyBuiltinLenApplicable : PySimpleConditionBase(ID) {
        companion object {
            private const val ID: String = "builtin len applicable"
        }
    }

    data class PyClassCondition(private val name: String) : PySimpleConditionBase(ID) {

        companion object {
            const val ID: String = "type"
        }
    }

    companion object {
        private fun getConditionsMap(vararg conditions: PyPostfixTemplateExpressionCondition): Map<String, PyPostfixTemplateExpressionCondition> {
            val result: MutableMap<String, PyPostfixTemplateExpressionCondition> = mutableMapOf()
            for (condition in conditions) {
                result[condition.id] = condition
            }
            return result
        }

        // conditions we allow to select in postfix template editor UI
        @JvmField
        val PUBLIC_CONDITIONS = getConditionsMap(
            PyBooleanExpression(),
            PyNumberExpression(),
            PyStringExpression(),
            PyIterable(),
            PyDict(),
            PyList(),
            PySet(),
            PyTuple(),
            PyNonNoneExpression(),
            PyExceptionExpression(),
        )
    }
}

/* GENERATED_FIR_TAGS: assignment, capturedType, classDeclaration, companionObject, const, data, equalityExpression,
forLoop, functionDeclaration, ifExpression, integerLiteral, interfaceDeclaration, localProperty, nestedClass,
nullableType, objectDeclaration, operator, outProjection, override, primaryConstructor, propertyDeclaration,
stringLiteral, thisExpression, vararg */
