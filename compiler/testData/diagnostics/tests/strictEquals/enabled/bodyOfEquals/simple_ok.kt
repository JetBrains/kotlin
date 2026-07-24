// RUN_PIPELINE_TILL: BACKEND

class Simple {
    val property: Int = 42
    override fun equals(@EqualityBound(Simple::class) other: Any?): Boolean {
        return property == other.property
    }
}

interface Parent {
    override fun equals(@EqualityBound(Parent::class) other: Any?): Boolean
}

fun Parent.doSomething() = Unit

class Child : Parent {
    override fun equals(@EqualityBound(Parent::class) other: Any?): Boolean {
        other.doSomething()
        return true
    }
}

class Generic<T> {
    val nonGenericField: String = "!"
    override fun equals(@EqualityBound(Generic::class) other: Any?): Boolean {
        return other.nonGenericField.length == nonGenericField.length
    }
}

fun local() {
    val str = "!"
    class Local {
        val fld = str
        override fun equals(@EqualityBound(Local::class) other: Any?): Boolean {
            return other.fld.length == fld.length
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, funWithExtensionReceiver,
functionDeclaration, integerLiteral, interfaceDeclaration, localClass, localProperty, nullableType, operator, override,
propertyDeclaration, smartcast, starProjection, stringLiteral, typeParameter */
